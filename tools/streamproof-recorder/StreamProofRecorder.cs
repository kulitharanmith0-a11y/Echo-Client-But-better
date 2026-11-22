using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using System.Windows.Forms;

namespace StreamProofRecorder
{
    internal static class Program
    {
        [STAThread]
        private static int Main(string[] args)
        {
            NativeMethods.SetProcessDPIAware();

            if (args.Length > 0 && string.Equals(args[0], "--self-test", StringComparison.OrdinalIgnoreCase))
            {
                return SelfTest.Run();
            }

            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new RecorderForm());
            return 0;
        }
    }

    internal enum CaptureMode
    {
        Auto = 0,
        DxgiDuplication = 1,
        GdiBitBlt = 2
    }

    internal sealed class WindowChoice
    {
        public IntPtr Handle { get; private set; }
        public string Title { get; private set; }
        public bool IsFullScreen { get { return Handle == IntPtr.Zero; } }

        public WindowChoice(IntPtr handle, string title)
        {
            Handle = handle;
            Title = title;
        }

        public override string ToString()
        {
            return Title;
        }
    }

    internal sealed class ScreenCaptureEngine : IDisposable
    {
        private readonly CaptureMode _requestedMode;
        private DxgiDuplicator _dxgi;
        private bool _dxgiAttempted;
        private string _activeBackend = "Initializing";
        private byte[] _frameBuffer;
        private int _frameWidth;
        private int _frameHeight;
        private byte[] _cropBuffer;

        public static int LastGdiError;
        public string ActiveBackend { get { return _activeBackend; } }

        public ScreenCaptureEngine(CaptureMode mode)
        {
            _requestedMode = mode;
        }

        public bool CaptureFrame(Rectangle region, out byte[] pixels, out int width, out int height)
        {
            pixels = null;
            width = region.Width & ~1;
            height = region.Height & ~1;
            if (width < 2 || height < 2)
            {
                return false;
            }

            Rectangle screen = SystemInformation.VirtualScreen;
            if (_requestedMode != CaptureMode.GdiBitBlt)
            {
                if (!_dxgiAttempted)
                {
                    _dxgiAttempted = true;
                    _dxgi = DxgiDuplicator.TryCreate(region);
                }

                if (_dxgi != null)
                {
                    if (_dxgi.Acquire(ref _frameBuffer, out _frameWidth, out _frameHeight))
                    {
                        _activeBackend = "DXGI Desktop Duplication";
                        Rectangle mon = _dxgi.MonitorBounds;
                        int relX = region.X - mon.X;
                        int relY = region.Y - mon.Y;
                        if (relX >= 0 && relY >= 0 && relX + width <= _frameWidth && relY + height <= _frameHeight)
                        {
                            if (relX == 0 && relY == 0 && width == _frameWidth && height == _frameHeight)
                            {
                                pixels = _frameBuffer;
                            }
                            else
                            {
                                CropTopDown(_frameBuffer, _frameWidth, relX, relY, width, height, ref _cropBuffer);
                                pixels = _cropBuffer;
                            }
                            return true;
                        }
                    }
                    else if (_dxgi.NeedsReset)
                    {
                        _dxgi.Dispose();
                        _dxgi = DxgiDuplicator.TryCreate(region);
                    }
                }
            }

            if (_requestedMode == CaptureMode.DxgiDuplication && _dxgi != null && DxgiDuplicator.LastDiag.Contains("WAIT_TIMEOUT"))
            {
                if (_dxgi.Acquire(ref _frameBuffer, out _frameWidth, out _frameHeight))
                {
                    _activeBackend = "DXGI Desktop Duplication";
                    Rectangle mon = _dxgi.MonitorBounds;
                    int relX = region.X - mon.X;
                    int relY = region.Y - mon.Y;
                    if (relX >= 0 && relY >= 0 && relX + width <= _frameWidth && relY + height <= _frameHeight)
                    {
                        if (relX == 0 && relY == 0 && width == _frameWidth && height == _frameHeight)
                        {
                            pixels = _frameBuffer;
                        }
                        else
                        {
                            CropTopDown(_frameBuffer, _frameWidth, relX, relY, width, height, ref _cropBuffer);
                            pixels = _cropBuffer;
                        }
                        return true;
                    }
                }
            }

            if (CaptureGdi(region.X, region.Y, width, height, ref _cropBuffer))
            {
                _activeBackend = _requestedMode == CaptureMode.DxgiDuplication
                    ? "GDI BitBlt (DXGI fallback)"
                    : "GDI BitBlt (CAPTUREBLT)";
                pixels = _cropBuffer;
                return true;
            }

            return false;
        }

        private static void CropTopDown(byte[] src, int srcWidth, int x, int y, int w, int h, ref byte[] dst)
        {
            int dstBytes = w * h * 4;
            if (dst == null || dst.Length < dstBytes)
            {
                dst = new byte[dstBytes];
            }
            int srcStride = srcWidth * 4;
            int dstStride = w * 4;
            for (int row = 0; row < h; row++)
            {
                Buffer.BlockCopy(src, (y + row) * srcStride + x * 4, dst, row * dstStride, dstStride);
            }
        }

        private static bool CaptureGdi(int x, int y, int width, int height, ref byte[] buffer)
        {
            IntPtr desktopDc = NativeMethods.GetDC(IntPtr.Zero);
            if (desktopDc == IntPtr.Zero)
            {
                return false;
            }
            IntPtr memDc = IntPtr.Zero;
            IntPtr hBitmap = IntPtr.Zero;
            IntPtr oldObj = IntPtr.Zero;
            try
            {
                memDc = NativeMethods.CreateCompatibleDC(desktopDc);
                if (memDc == IntPtr.Zero)
                {
                    return false;
                }

                NativeMethods.BITMAPINFO bmi = new NativeMethods.BITMAPINFO();
                bmi.bmiHeader.biSize = Marshal.SizeOf(typeof(NativeMethods.BITMAPINFOHEADER));
                bmi.bmiHeader.biWidth = width;
                bmi.bmiHeader.biHeight = -height; // Top-down DIB
                bmi.bmiHeader.biPlanes = 1;
                bmi.bmiHeader.biBitCount = 32;
                bmi.bmiHeader.biCompression = NativeMethods.BI_RGB;

                IntPtr bits;
                hBitmap = NativeMethods.CreateDIBSection(memDc, ref bmi, 0, out bits, IntPtr.Zero, 0);
                if (hBitmap == IntPtr.Zero || bits == IntPtr.Zero)
                {
                    return false;
                }

                oldObj = NativeMethods.SelectObject(memDc, hBitmap);
                bool ok = NativeMethods.BitBlt(
                    memDc, 0, 0, width, height,
                    desktopDc, x, y,
                    NativeMethods.SRCCOPY | NativeMethods.CAPTUREBLT);
                if (!ok)
                {
                    LastGdiError = Marshal.GetLastWin32Error();
                    return false;
                }

                int byteCount = width * height * 4;
                if (buffer == null || buffer.Length < byteCount)
                {
                    buffer = new byte[byteCount];
                }
                Marshal.Copy(bits, buffer, 0, byteCount);
                return true;
            }
            finally
            {
                if (oldObj != IntPtr.Zero && memDc != IntPtr.Zero) NativeMethods.SelectObject(memDc, oldObj);
                if (hBitmap != IntPtr.Zero) NativeMethods.DeleteObject(hBitmap);
                if (memDc != IntPtr.Zero) NativeMethods.DeleteDC(memDc);
                NativeMethods.ReleaseDC(IntPtr.Zero, desktopDc);
            }
        }

        public void Dispose()
        {
            if (_dxgi != null)
            {
                _dxgi.Dispose();
                _dxgi = null;
            }
        }
    }

    internal sealed class DxgiDuplicator : IDisposable
    {
        private IntPtr _device;
        private IntPtr _context;
        private IntPtr _dupl;
        private IntPtr _staging;
        private int _width;
        private int _height;
        private Rectangle _monitorBounds;
        private bool _hasFrame;
        private bool _needsReset;

        public Rectangle MonitorBounds { get { return _monitorBounds; } }
        public bool NeedsReset { get { return _needsReset; } }
        public static string LastDiag = "none";

        public static DxgiDuplicator TryCreate(Rectangle targetRegion)
        {
            IntPtr factory = IntPtr.Zero;
            IntPtr device = IntPtr.Zero;
            IntPtr context = IntPtr.Zero;
            IntPtr chosenAdapter = IntPtr.Zero;
            IntPtr chosenOutput = IntPtr.Zero;
            IntPtr output1 = IntPtr.Zero;
            IntPtr dupl = IntPtr.Zero;

            try
            {
                Guid iidFactory1 = new Guid("770aae78-f26f-4dba-a829-253c83d1b387");
                int hr = NativeMethods.CreateDXGIFactory1(ref iidFactory1, out factory);
                if (hr < 0 || factory == IntPtr.Zero)
                {
                    LastDiag = string.Format("CreateDXGIFactory1 failed hr=0x{0:X8}", hr);
                    return null;
                }

                Point center = new Point(targetRegion.X + targetRegion.Width / 2, targetRegion.Y + targetRegion.Height / 2);
                Rectangle bestBounds = Screen.PrimaryScreen.Bounds;

                for (uint aIdx = 0; aIdx < 8; aIdx++)
                {
                    IntPtr adapter;
                    if (ComHelper.EnumAdapters(factory, aIdx, out adapter) < 0 || adapter == IntPtr.Zero)
                    {
                        break;
                    }
                    bool foundExact = false;
                    for (uint oIdx = 0; oIdx < 8; oIdx++)
                    {
                        IntPtr candidate;
                        if (ComHelper.EnumOutputs(adapter, oIdx, out candidate) < 0 || candidate == IntPtr.Zero)
                        {
                            break;
                        }
                        NativeMethods.DXGI_OUTPUT_DESC desc;
                        if (ComHelper.GetOutputDesc(candidate, out desc) >= 0 && desc.AttachedToDesktop != 0)
                        {
                            Rectangle r = Rectangle.FromLTRB(
                                desc.DesktopCoordinates.Left,
                                desc.DesktopCoordinates.Top,
                                desc.DesktopCoordinates.Right,
                                desc.DesktopCoordinates.Bottom);
                            if (chosenOutput == IntPtr.Zero || r.Contains(center))
                            {
                                if (chosenOutput != IntPtr.Zero) ComHelper.Release(chosenOutput);
                                if (chosenAdapter != IntPtr.Zero) ComHelper.Release(chosenAdapter);
                                ComHelper.AddRef(adapter);
                                chosenAdapter = adapter;
                                chosenOutput = candidate;
                                bestBounds = r;
                                if (r.Contains(center))
                                {
                                    foundExact = true;
                                    break;
                                }
                                continue;
                            }
                        }
                        ComHelper.Release(candidate);
                    }
                    ComHelper.Release(adapter);
                    if (foundExact) break;
                }

                if (chosenOutput == IntPtr.Zero || chosenAdapter == IntPtr.Zero)
                {
                    LastDiag = "No attached DXGI output found";
                    return null;
                }

                int featureLevel;
                hr = NativeMethods.D3D11CreateDevice(
                    chosenAdapter,
                    0, // D3D_DRIVER_TYPE_UNKNOWN when adapter is non-null
                    IntPtr.Zero,
                    0,
                    IntPtr.Zero,
                    0,
                    7, // D3D11_SDK_VERSION
                    out device,
                    out featureLevel,
                    out context);
                if (hr < 0 || device == IntPtr.Zero || context == IntPtr.Zero)
                {
                    LastDiag = string.Format("D3D11CreateDevice failed hr=0x{0:X8}", hr);
                    return null;
                }

                Guid iidOutput1 = new Guid("00cddea8-939b-4b83-a340-a685226666cc");
                hr = ComHelper.QueryInterface(chosenOutput, ref iidOutput1, out output1);
                if (hr < 0 || output1 == IntPtr.Zero)
                {
                    LastDiag = string.Format("QI IDXGIOutput1 failed hr=0x{0:X8}", hr);
                    return null;
                }

                // IDXGIOutput1::DuplicateOutput is slot 22
                hr = ComHelper.DuplicateOutput(output1, device, out dupl);
                if (hr < 0 || dupl == IntPtr.Zero)
                {
                    LastDiag = string.Format("DuplicateOutput failed hr=0x{0:X8}", hr);
                    return null;
                }

                DxgiDuplicator result = new DxgiDuplicator();
                result._device = device; device = IntPtr.Zero;
                result._context = context; context = IntPtr.Zero;
                result._dupl = dupl; dupl = IntPtr.Zero;
                result._monitorBounds = bestBounds;
                result._width = bestBounds.Width;
                result._height = bestBounds.Height;
                LastDiag = "OK";
                return result;
            }
            catch (Exception ex)
            {
                LastDiag = "Exception: " + ex.Message;
                return null;
            }
            finally
            {
                if (dupl != IntPtr.Zero) ComHelper.Release(dupl);
                if (output1 != IntPtr.Zero) ComHelper.Release(output1);
                if (chosenOutput != IntPtr.Zero) ComHelper.Release(chosenOutput);
                if (chosenAdapter != IntPtr.Zero) ComHelper.Release(chosenAdapter);
                if (context != IntPtr.Zero) ComHelper.Release(context);
                if (device != IntPtr.Zero) ComHelper.Release(device);
                if (factory != IntPtr.Zero) ComHelper.Release(factory);
            }
        }

        public bool Acquire(ref byte[] buffer, out int width, out int height)
        {
            width = _width;
            height = _height;
            if (_dupl == IntPtr.Zero)
            {
                _needsReset = true;
                return false;
            }

            IntPtr desktopResource = IntPtr.Zero;
            IntPtr tex = IntPtr.Zero;
            bool frameAcquired = false;

            try
            {
                NativeMethods.DXGI_OUTDUPL_FRAME_INFO info;
                int hr = ComHelper.AcquireNextFrame(_dupl, 100, out info, out desktopResource);
                if (hr == unchecked((int)0x887A0027)) // DXGI_ERROR_WAIT_TIMEOUT
                {
                    LastDiag = "Acquire WAIT_TIMEOUT hasFrame=" + _hasFrame;
                    return _hasFrame && buffer != null;
                }
                if (hr == unchecked((int)0x887A0026) || hr < 0) // DXGI_ERROR_ACCESS_LOST or failure
                {
                    LastDiag = string.Format("AcquireNextFrame failed hr=0x{0:X8}", hr);
                    _needsReset = true;
                    return false;
                }
                frameAcquired = true;

                Guid iidTex2D = new Guid("6f15aaf2-d208-4e89-9ab4-489535d34f9c");
                hr = ComHelper.QueryInterface(desktopResource, ref iidTex2D, out tex);
                if (hr < 0 || tex == IntPtr.Zero)
                {
                    return false;
                }

                NativeMethods.D3D11_TEXTURE2D_DESC desc;
                ComHelper.GetTexture2DDesc(tex, out desc);
                _width = (int)desc.Width;
                _height = (int)desc.Height;
                width = _width;
                height = _height;

                if (_staging == IntPtr.Zero || _width <= 0 || _height <= 0)
                {
                    desc.Usage = 3; // D3D11_USAGE_STAGING
                    desc.BindFlags = 0;
                    desc.CPUAccessFlags = 0x20000; // D3D11_CPU_ACCESS_READ
                    desc.MiscFlags = 0;
                    desc.MipLevels = 1;
                    desc.ArraySize = 1;
                    desc.SampleCount = 1;
                    desc.SampleQuality = 0;
                    hr = ComHelper.CreateTexture2D(_device, ref desc, out _staging);
                    if (hr < 0 || _staging == IntPtr.Zero)
                    {
                        return false;
                    }
                }

                ComHelper.CopyResource(_context, _staging, tex);

                NativeMethods.D3D11_MAPPED_SUBRESOURCE mapped;
                hr = ComHelper.Map(_context, _staging, 0, 1 /* D3D11_MAP_READ */, 0, out mapped);
                if (hr < 0 || mapped.pData == IntPtr.Zero)
                {
                    return false;
                }

                try
                {
                    int rowBytes = _width * 4;
                    int totalBytes = rowBytes * _height;
                    if (buffer == null || buffer.Length < totalBytes)
                    {
                        buffer = new byte[totalBytes];
                    }
                    int pitch = (int)mapped.RowPitch;
                    if (pitch == rowBytes)
                    {
                        Marshal.Copy(mapped.pData, buffer, 0, totalBytes);
                    }
                    else
                    {
                        for (int y = 0; y < _height; y++)
                        {
                            Marshal.Copy(IntPtr.Add(mapped.pData, y * pitch), buffer, y * rowBytes, rowBytes);
                        }
                    }
                    _hasFrame = true;
                    return true;
                }
                finally
                {
                    ComHelper.Unmap(_context, _staging, 0);
                }
            }
            finally
            {
                if (tex != IntPtr.Zero) ComHelper.Release(tex);
                if (desktopResource != IntPtr.Zero) ComHelper.Release(desktopResource);
                if (frameAcquired && _dupl != IntPtr.Zero)
                {
                    ComHelper.ReleaseFrame(_dupl);
                }
            }
        }

        public void Dispose()
        {
            if (_staging != IntPtr.Zero) { ComHelper.Release(_staging); _staging = IntPtr.Zero; }
            if (_dupl != IntPtr.Zero) { ComHelper.Release(_dupl); _dupl = IntPtr.Zero; }
            if (_context != IntPtr.Zero) { ComHelper.Release(_context); _context = IntPtr.Zero; }
            if (_device != IntPtr.Zero) { ComHelper.Release(_device); _device = IntPtr.Zero; }
        }
    }

    internal sealed class Mp4VideoWriter : IDisposable
    {
        private IntPtr _sinkWriter;
        private uint _streamIndex;
        private readonly int _width;
        private readonly int _height;
        private readonly long _frameDurationHns;
        private bool _mfStarted;
        private bool _finalized;

        public Mp4VideoWriter(string outputPath, int width, int height, int fps, int bitrate)
        {
            _width = width;
            _height = height;
            _frameDurationHns = 10000000L / fps;

            int hr = NativeMethods.MFStartup(0x00020070u, 0u);
            if (hr < 0) Marshal.ThrowExceptionForHR(hr);
            _mfStarted = true;

            IntPtr attrs = IntPtr.Zero;
            IntPtr mediaTypeOut = IntPtr.Zero;
            IntPtr mediaTypeIn = IntPtr.Zero;

            try
            {
                hr = NativeMethods.MFCreateAttributes(out attrs, 2);
                if (hr < 0) Marshal.ThrowExceptionForHR(hr);

                Guid mfReadWriteHw = new Guid("a634a91c-822b-41b9-a494-4de4643612b0");
                ComHelper.SetUINT32(attrs, ref mfReadWriteHw, 1u);

                hr = NativeMethods.MFCreateSinkWriterFromURL(outputPath, IntPtr.Zero, attrs, out _sinkWriter);
                if (hr < 0) Marshal.ThrowExceptionForHR(hr);

                Guid majorVideo = new Guid("73646976-0000-0010-8000-00AA00389B71");
                Guid subH264 = new Guid("34363248-0000-0010-8000-00AA00389B71");
                Guid subRgb32 = new Guid("00000016-0000-0010-8000-00AA00389B71");
                Guid mtMajorType = new Guid("48eba18e-f8c9-4687-bf11-0a74c9f96a8f");
                Guid mtSubtype = new Guid("f7e34c9a-42e8-4714-b74b-cb29d72c35e5");
                Guid mtAvgBitrate = new Guid("20332624-fb0d-4d9e-bd0d-cbf6786c102e");
                Guid mtInterlace = new Guid("e2724bb8-e676-4806-b4b2-a8d6efb44ccd");
                Guid mtFrameSize = new Guid("1652c33d-d6b2-4012-b834-72030849a37d");
                Guid mtFrameRate = new Guid("c459a2e8-3d2c-4e44-b132-fef5156c7bb0");
                Guid mtPixelAspect = new Guid("c6376a1e-8d0a-4027-be45-6d9a0ad39bb6");
                Guid mtDefaultStride = new Guid("644b4e48-1e02-4516-b0eb-67c1f07fa621");

                ulong frameSize = ((ulong)(uint)width << 32) | (uint)height;
                ulong frameRate = ((ulong)(uint)fps << 32) | 1u;
                ulong aspect = (1UL << 32) | 1u;

                hr = NativeMethods.MFCreateMediaType(out mediaTypeOut);
                if (hr < 0) Marshal.ThrowExceptionForHR(hr);
                ComHelper.SetGUID(mediaTypeOut, ref mtMajorType, ref majorVideo);
                ComHelper.SetGUID(mediaTypeOut, ref mtSubtype, ref subH264);
                ComHelper.SetUINT32(mediaTypeOut, ref mtAvgBitrate, (uint)bitrate);
                ComHelper.SetUINT32(mediaTypeOut, ref mtInterlace, 2u);
                ComHelper.SetUINT64(mediaTypeOut, ref mtFrameSize, frameSize);
                ComHelper.SetUINT64(mediaTypeOut, ref mtFrameRate, frameRate);
                ComHelper.SetUINT64(mediaTypeOut, ref mtPixelAspect, aspect);

                hr = ComHelper.AddStream(_sinkWriter, mediaTypeOut, out _streamIndex);
                if (hr < 0) Marshal.ThrowExceptionForHR(hr);

                hr = NativeMethods.MFCreateMediaType(out mediaTypeIn);
                if (hr < 0) Marshal.ThrowExceptionForHR(hr);
                ComHelper.SetGUID(mediaTypeIn, ref mtMajorType, ref majorVideo);
                ComHelper.SetGUID(mediaTypeIn, ref mtSubtype, ref subRgb32);
                ComHelper.SetUINT32(mediaTypeIn, ref mtInterlace, 2u);
                ComHelper.SetUINT64(mediaTypeIn, ref mtFrameSize, frameSize);
                ComHelper.SetUINT64(mediaTypeIn, ref mtFrameRate, frameRate);
                ComHelper.SetUINT64(mediaTypeIn, ref mtPixelAspect, aspect);
                ComHelper.SetUINT32(mediaTypeIn, ref mtDefaultStride, (uint)(width * 4));

                hr = ComHelper.SetInputMediaType(_sinkWriter, _streamIndex, mediaTypeIn, IntPtr.Zero);
                if (hr < 0) Marshal.ThrowExceptionForHR(hr);

                hr = ComHelper.BeginWriting(_sinkWriter);
                if (hr < 0) Marshal.ThrowExceptionForHR(hr);
            }
            finally
            {
                if (mediaTypeIn != IntPtr.Zero) ComHelper.Release(mediaTypeIn);
                if (mediaTypeOut != IntPtr.Zero) ComHelper.Release(mediaTypeOut);
                if (attrs != IntPtr.Zero) ComHelper.Release(attrs);
            }
        }

        public void WriteFrame(byte[] bgraTopDown, long timestampHns)
        {
            if (_sinkWriter == IntPtr.Zero || _finalized)
            {
                return;
            }

            int byteCount = _width * _height * 4;
            IntPtr sample = IntPtr.Zero;
            IntPtr mediaBuffer = IntPtr.Zero;

            try
            {
                int hr = NativeMethods.MFCreateMemoryBuffer((uint)byteCount, out mediaBuffer);
                if (hr < 0) Marshal.ThrowExceptionForHR(hr);

                IntPtr dstPtr;
                uint maxLen, curLen;
                hr = ComHelper.LockBuffer(mediaBuffer, out dstPtr, out maxLen, out curLen);
                if (hr < 0) Marshal.ThrowExceptionForHR(hr);

                try
                {
                    Marshal.Copy(bgraTopDown, 0, dstPtr, byteCount);
                }
                finally
                {
                    ComHelper.UnlockBuffer(mediaBuffer);
                }

                hr = ComHelper.SetCurrentLength(mediaBuffer, (uint)byteCount);
                if (hr < 0) Marshal.ThrowExceptionForHR(hr);

                hr = NativeMethods.MFCreateSample(out sample);
                if (hr < 0) Marshal.ThrowExceptionForHR(hr);

                hr = ComHelper.AddBuffer(sample, mediaBuffer);
                if (hr < 0) Marshal.ThrowExceptionForHR(hr);

                hr = ComHelper.SetSampleTime(sample, timestampHns);
                if (hr < 0) Marshal.ThrowExceptionForHR(hr);

                hr = ComHelper.SetSampleDuration(sample, _frameDurationHns);
                if (hr < 0) Marshal.ThrowExceptionForHR(hr);

                hr = ComHelper.WriteSample(_sinkWriter, _streamIndex, sample);
                if (hr < 0) Marshal.ThrowExceptionForHR(hr);
            }
            finally
            {
                if (mediaBuffer != IntPtr.Zero) ComHelper.Release(mediaBuffer);
                if (sample != IntPtr.Zero) ComHelper.Release(sample);
            }
        }

        public void Complete()
        {
            if (!_finalized && _sinkWriter != IntPtr.Zero)
            {
                _finalized = true;
                ComHelper.FinalizeWriter(_sinkWriter);
            }
        }

        public void Dispose()
        {
            try
            {
                Complete();
            }
            catch
            {
            }
            if (_sinkWriter != IntPtr.Zero)
            {
                ComHelper.Release(_sinkWriter);
                _sinkWriter = IntPtr.Zero;
            }
            if (_mfStarted)
            {
                _mfStarted = false;
                NativeMethods.MFShutdown();
            }
        }
    }

    internal sealed class RecorderForm : Form
    {
        private const int HOTKEY_RECORD = 0x101;
        private const int HOTKEY_SNAPSHOT = 0x102;
        private const int WM_HOTKEY = 0x0312;

        private readonly ComboBox _modeCombo;
        private readonly ComboBox _windowCombo;
        private readonly Button _refreshBtn;
        private readonly CheckBox _excludeSelfBox;
        private readonly CheckBox _minimizeBox;
        private readonly CheckBox _previewBox;
        private readonly Button _recordBtn;
        private readonly Button _snapshotBtn;
        private readonly Button _playBtn;
        private readonly Button _openFolderBtn;
        private readonly Label _statusLabel;
        private readonly Label _backendLabel;
        private readonly PictureBox _previewPicture;

        private readonly string _outputDir;
        private Thread _recordThread;
        private volatile bool _stopRequested;
        private volatile bool _isRecording;
        private volatile string _lastVideoPath;
        private volatile string _statusText = "Ready (F9: Record/Stop, F10: Snapshot)";
        private volatile string _backendText = "Backend: Idle";
        private readonly object _previewLock = new object();
        private Bitmap _latestPreviewBitmap;
        private readonly System.Windows.Forms.Timer _uiTimer;

        public RecorderForm()
        {
            Text = "StreamProof Test Recorder";
            ClientSize = new Size(560, 480);
            MinimumSize = new Size(520, 440);
            FormBorderStyle = FormBorderStyle.Sizable;
            BackColor = Color.FromArgb(20, 20, 24);
            ForeColor = Color.FromArgb(235, 235, 242);
            Font = new Font("Segoe UI", 9.5f);

            string baseDir = AppDomain.CurrentDomain.BaseDirectory;
            _outputDir = Path.Combine(baseDir, "recordings");
            Directory.CreateDirectory(_outputDir);

            Panel controlsPanel = new Panel
            {
                Dock = DockStyle.Top,
                Height = 178,
                Padding = new Padding(14, 12, 14, 8),
                BackColor = Color.FromArgb(26, 26, 32)
            };

            Label modeLbl = new Label
            {
                Text = "Capture API:",
                Location = new Point(14, 15),
                AutoSize = true,
                ForeColor = Color.FromArgb(180, 185, 198)
            };
            _modeCombo = new ComboBox
            {
                Location = new Point(112, 11),
                Width = 230,
                DropDownStyle = ComboBoxStyle.DropDownList,
                BackColor = Color.FromArgb(36, 36, 44),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            _modeCombo.Items.AddRange(new object[]
            {
                "Auto (DXGI Duplication -> GDI)",
                "DXGI Desktop Duplication (OBS/Discord)",
                "GDI BitBlt (SRCCOPY | CAPTUREBLT)"
            });
            _modeCombo.SelectedIndex = 0;

            Label targetLbl = new Label
            {
                Text = "Target Region:",
                Location = new Point(14, 48),
                AutoSize = true,
                ForeColor = Color.FromArgb(180, 185, 198)
            };
            _windowCombo = new ComboBox
            {
                Location = new Point(112, 44),
                Width = 340,
                DropDownStyle = ComboBoxStyle.DropDownList,
                BackColor = Color.FromArgb(36, 36, 44),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            _refreshBtn = MakeButton("Refresh", new Point(460, 43), new Size(84, 27), Color.FromArgb(52, 54, 68));
            _refreshBtn.Click += (s, e) => PopulateWindows();

            _excludeSelfBox = new CheckBox
            {
                Text = "Hide recorder from capture",
                Location = new Point(14, 79),
                AutoSize = true,
                Checked = true,
                ForeColor = Color.FromArgb(215, 220, 230)
            };
            _excludeSelfBox.CheckedChanged += (s, e) => ApplySelfAffinity();

            _minimizeBox = new CheckBox
            {
                Text = "Minimize on record",
                Location = new Point(218, 79),
                AutoSize = true,
                Checked = false,
                ForeColor = Color.FromArgb(215, 220, 230)
            };

            _previewBox = new CheckBox
            {
                Text = "Live preview",
                Location = new Point(378, 79),
                AutoSize = true,
                Checked = true,
                ForeColor = Color.FromArgb(215, 220, 230)
            };

            _recordBtn = MakeButton("Record (F9)", new Point(14, 110), new Size(135, 34), Color.FromArgb(196, 56, 68));
            _recordBtn.Font = new Font("Segoe UI", 9.5f, FontStyle.Bold);
            _recordBtn.Click += (s, e) => ToggleRecording();

            _snapshotBtn = MakeButton("Snapshot (F10)", new Point(157, 110), new Size(125, 34), Color.FromArgb(56, 108, 178));
            _snapshotBtn.Click += (s, e) => TakeSnapshot();

            _playBtn = MakeButton("Play Last MP4", new Point(290, 110), new Size(122, 34), Color.FromArgb(52, 54, 68));
            _playBtn.Enabled = false;
            _playBtn.Click += (s, e) => PlayLastVideo();

            _openFolderBtn = MakeButton("Open Folder", new Point(420, 110), new Size(124, 34), Color.FromArgb(52, 54, 68));
            _openFolderBtn.Click += (s, e) => OpenOutputFolder();

            _statusLabel = new Label
            {
                Text = _statusText,
                Location = new Point(14, 152),
                Size = new Size(330, 20),
                ForeColor = Color.FromArgb(145, 220, 175)
            };
            _backendLabel = new Label
            {
                Text = _backendText,
                Location = new Point(345, 152),
                Size = new Size(202, 20),
                TextAlign = ContentAlignment.MiddleRight,
                ForeColor = Color.FromArgb(150, 160, 180)
            };

            controlsPanel.Controls.AddRange(new Control[]
            {
                modeLbl, _modeCombo,
                targetLbl, _windowCombo, _refreshBtn,
                _excludeSelfBox, _minimizeBox, _previewBox,
                _recordBtn, _snapshotBtn, _playBtn, _openFolderBtn,
                _statusLabel, _backendLabel
            });

            _previewPicture = new PictureBox
            {
                Dock = DockStyle.Fill,
                BackColor = Color.FromArgb(12, 12, 16),
                SizeMode = PictureBoxSizeMode.Zoom
            };

            Controls.Add(_previewPicture);
            Controls.Add(controlsPanel);

            PopulateWindows();

            _uiTimer = new System.Windows.Forms.Timer { Interval = 120 };
            _uiTimer.Tick += OnUiTick;
            _uiTimer.Start();

            Load += (s, e) =>
            {
                ApplySelfAffinity();
                NativeMethods.RegisterHotKey(Handle, HOTKEY_RECORD, 0, (uint)Keys.F9);
                NativeMethods.RegisterHotKey(Handle, HOTKEY_SNAPSHOT, 0, (uint)Keys.F10);
            };
            FormClosing += (s, e) =>
            {
                _uiTimer.Stop();
                NativeMethods.UnregisterHotKey(Handle, HOTKEY_RECORD);
                NativeMethods.UnregisterHotKey(Handle, HOTKEY_SNAPSHOT);
                StopRecordingBlocking();
                lock (_previewLock)
                {
                    if (_latestPreviewBitmap != null) { _latestPreviewBitmap.Dispose(); _latestPreviewBitmap = null; }
                }
            };
        }

        private static Button MakeButton(string text, Point location, Size size, Color backColor)
        {
            Button btn = new Button
            {
                Text = text,
                Location = location,
                Size = size,
                BackColor = backColor,
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat,
                Cursor = Cursors.Hand
            };
            btn.FlatAppearance.BorderSize = 0;
            return btn;
        }

        private void ApplySelfAffinity()
        {
            if (IsHandleCreated)
            {
                uint affinity = _excludeSelfBox.Checked ? 0x11u : 0x00u;
                NativeMethods.SetWindowDisplayAffinity(Handle, affinity);
            }
        }

        protected override void WndProc(ref Message m)
        {
            if (m.Msg == WM_HOTKEY)
            {
                int id = m.WParam.ToInt32();
                if (id == HOTKEY_RECORD)
                {
                    ToggleRecording();
                    return;
                }
                if (id == HOTKEY_SNAPSHOT)
                {
                    TakeSnapshot();
                    return;
                }
            }
            base.WndProc(ref m);
        }

        private void PopulateWindows()
        {
            IntPtr selectedHandle = IntPtr.Zero;
            WindowChoice current = _windowCombo.SelectedItem as WindowChoice;
            if (current != null) selectedHandle = current.Handle;

            List<WindowChoice> list = new List<WindowChoice>();
            list.Add(new WindowChoice(IntPtr.Zero, "Full Primary Monitor"));

            List<WindowChoice> windows = new List<WindowChoice>();
            NativeMethods.EnumWindows((hwnd, lParam) =>
            {
                if (hwnd == Handle || !NativeMethods.IsWindowVisible(hwnd) || NativeMethods.IsIconic(hwnd))
                {
                    return true;
                }
                int len = NativeMethods.GetWindowTextLength(hwnd);
                if (len <= 0) return true;
                StringBuilder sb = new StringBuilder(len + 1);
                NativeMethods.GetWindowText(hwnd, sb, sb.Capacity);
                string title = sb.ToString().Trim();
                if (string.IsNullOrEmpty(title)) return true;
                Rectangle rect = GetTargetWindowBounds(hwnd);
                if (rect.Width < 100 || rect.Height < 100) return true;
                windows.Add(new WindowChoice(hwnd, title));
                return true;
            }, IntPtr.Zero);

            windows.Sort((a, b) =>
            {
                bool aMc = a.Title.IndexOf("Minecraft", StringComparison.OrdinalIgnoreCase) >= 0;
                bool bMc = b.Title.IndexOf("Minecraft", StringComparison.OrdinalIgnoreCase) >= 0;
                if (aMc != bMc) return aMc ? -1 : 1;
                return string.Compare(a.Title, b.Title, StringComparison.OrdinalIgnoreCase);
            });
            list.AddRange(windows);

            _windowCombo.Items.Clear();
            int restoreIndex = 0;
            for (int i = 0; i < list.Count; i++)
            {
                _windowCombo.Items.Add(list[i]);
                if (list[i].Handle == selectedHandle) restoreIndex = i;
            }
            _windowCombo.SelectedIndex = restoreIndex;
        }

        private Rectangle GetCaptureRegion(WindowChoice target)
        {
            if (target == null || target.IsFullScreen || !NativeMethods.IsWindow(target.Handle))
            {
                return Screen.PrimaryScreen.Bounds;
            }
            Rectangle win = GetTargetWindowBounds(target.Handle);
            Rectangle desk = SystemInformation.VirtualScreen;
            Rectangle clipped = Rectangle.Intersect(win, desk);
            if (clipped.Width < 16 || clipped.Height < 16)
            {
                return Screen.PrimaryScreen.Bounds;
            }
            return clipped;
        }

        private static Rectangle GetTargetWindowBounds(IntPtr hwnd)
        {
            NativeMethods.RECT r;
            int hr = NativeMethods.DwmGetWindowAttribute(hwnd, 9 /* DWMWA_EXTENDED_FRAME_BOUNDS */, out r, Marshal.SizeOf(typeof(NativeMethods.RECT)));
            if (hr < 0)
            {
                NativeMethods.GetWindowRect(hwnd, out r);
            }
            return Rectangle.FromLTRB(r.Left, r.Top, r.Right, r.Bottom);
        }

        private void OnUiTick(object sender, EventArgs e)
        {
            _statusLabel.Text = _statusText;
            _backendLabel.Text = _backendText;
            _playBtn.Enabled = !_isRecording && !string.IsNullOrEmpty(_lastVideoPath) && File.Exists(_lastVideoPath);

            if (_isRecording)
            {
                lock (_previewLock)
                {
                    if (_latestPreviewBitmap != null)
                    {
                        Image old = _previewPicture.Image;
                        _previewPicture.Image = (Bitmap)_latestPreviewBitmap.Clone();
                        if (old != null) old.Dispose();
                    }
                }
                return;
            }

            if (_previewBox.Checked && WindowState != FormWindowState.Minimized)
            {
                UpdateIdlePreview();
            }
        }

        private ScreenCaptureEngine _idlePreviewEngine;
        private CaptureMode _idleMode = (CaptureMode)(-1);

        private void UpdateIdlePreview()
        {
            CaptureMode mode = (CaptureMode)_modeCombo.SelectedIndex;
            if (_idlePreviewEngine == null || _idleMode != mode)
            {
                if (_idlePreviewEngine != null) _idlePreviewEngine.Dispose();
                _idlePreviewEngine = new ScreenCaptureEngine(mode);
                _idleMode = mode;
            }
            WindowChoice target = _windowCombo.SelectedItem as WindowChoice;
            Rectangle region = GetCaptureRegion(target);
            byte[] pixels;
            int w, h;
            if (_idlePreviewEngine.CaptureFrame(region, out pixels, out w, out h))
            {
                _backendText = _idlePreviewEngine.ActiveBackend;
                Bitmap bmp = PixelsToBitmap(pixels, w, h);
                Image old = _previewPicture.Image;
                _previewPicture.Image = bmp;
                if (old != null) old.Dispose();
            }
            else
            {
                _backendText = "Waiting for frame...";
            }
        }

        private void ToggleRecording()
        {
            if (_isRecording)
            {
                StopRecordingAsync();
            }
            else
            {
                StartRecording();
            }
        }

        private void StartRecording()
        {
            if (_idlePreviewEngine != null)
            {
                _idlePreviewEngine.Dispose();
                _idlePreviewEngine = null;
            }

            CaptureMode mode = (CaptureMode)_modeCombo.SelectedIndex;
            WindowChoice target = _windowCombo.SelectedItem as WindowChoice;
            Rectangle initialRegion = GetCaptureRegion(target);
            int fixedWidth = (initialRegion.Width & ~1);
            int fixedHeight = (initialRegion.Height & ~1);
            if (fixedWidth < 2 || fixedHeight < 2)
            {
                MessageBox.Show("Invalid capture region size.", "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return;
            }

            string outFile = Path.Combine(_outputDir, "capture_" + DateTime.Now.ToString("yyyyMMdd_HHmmss") + ".mp4");
            _stopRequested = false;
            _isRecording = true;

            _modeCombo.Enabled = false;
            _windowCombo.Enabled = false;
            _refreshBtn.Enabled = false;
            _recordBtn.Text = "Stop (F9)";
            _recordBtn.BackColor = Color.FromArgb(228, 92, 52);
            _statusText = "Starting encoder...";

            if (_minimizeBox.Checked)
            {
                WindowState = FormWindowState.Minimized;
            }

            IntPtr targetHwnd = target != null ? target.Handle : IntPtr.Zero;
            bool showPreview = _previewBox.Checked;

            _recordThread = new Thread(() => RecordWorker(outFile, mode, targetHwnd, initialRegion, fixedWidth, fixedHeight, showPreview));
            _recordThread.IsBackground = true;
            _recordThread.SetApartmentState(ApartmentState.MTA);
            _recordThread.Start();
        }

        private void RecordWorker(
            string outFile, CaptureMode mode, IntPtr targetHwnd,
            Rectangle initialRegion, int fixedWidth, int fixedHeight, bool showPreview)
        {
            const int fps = 30;
            Stopwatch sw = new Stopwatch();
            int framesWritten = 0;

            try
            {
                using (ScreenCaptureEngine capturer = new ScreenCaptureEngine(mode))
                using (Mp4VideoWriter writer = new Mp4VideoWriter(outFile, fixedWidth, fixedHeight, fps, 12000000))
                {
                    sw.Start();
                    long frameIndex = 0;
                    long lastPts = -1;
                    int previewTick = 0;

                    while (!_stopRequested)
                    {
                        Rectangle region = initialRegion;
                        if (targetHwnd != IntPtr.Zero && NativeMethods.IsWindow(targetHwnd))
                        {
                            Rectangle current = GetTargetWindowBounds(targetHwnd);
                            Rectangle desk = SystemInformation.VirtualScreen;
                            Rectangle clipped = Rectangle.Intersect(current, desk);
                            if (clipped.Width >= fixedWidth && clipped.Height >= fixedHeight)
                            {
                                region = new Rectangle(clipped.X, clipped.Y, fixedWidth, fixedHeight);
                            }
                            else
                            {
                                region = new Rectangle(initialRegion.X, initialRegion.Y, fixedWidth, fixedHeight);
                            }
                        }

                        byte[] pixels;
                        int w, h;
                        if (capturer.CaptureFrame(region, out pixels, out w, out h) && w == fixedWidth && h == fixedHeight)
                        {
                            long nowHns = sw.ElapsedTicks * 10000000L / Stopwatch.Frequency;
                            long pts = lastPts < 0 ? 0L : Math.Max(lastPts + 10000L, nowHns);
                            writer.WriteFrame(pixels, pts);
                            lastPts = pts;
                            framesWritten++;
                            _backendText = capturer.ActiveBackend;

                            if (showPreview && (++previewTick % 4 == 0))
                            {
                                Bitmap bmp = PixelsToBitmap(pixels, w, h);
                                lock (_previewLock)
                                {
                                    if (_latestPreviewBitmap != null) _latestPreviewBitmap.Dispose();
                                    _latestPreviewBitmap = bmp;
                                }
                            }
                        }

                        frameIndex++;
                        double elapsedSec = sw.Elapsed.TotalSeconds;
                        _statusText = string.Format("REC {0:00}:{1:00} | {2} frames",
                            (int)elapsedSec / 60, (int)elapsedSec % 60, framesWritten);

                        long targetTicks = frameIndex * Stopwatch.Frequency / fps;
                        long remainingMs = (targetTicks - sw.ElapsedTicks) * 1000L / Stopwatch.Frequency;
                        if (remainingMs > 1)
                        {
                            Thread.Sleep((int)Math.Min(remainingMs, 33));
                        }
                    }

                    writer.Complete();
                    _lastVideoPath = outFile;
                    _statusText = string.Format("Saved {0} ({1} frames)", Path.GetFileName(outFile), framesWritten);
                }
            }
            catch (Exception ex)
            {
                _statusText = "Error: " + ex.Message;
            }
            finally
            {
                _isRecording = false;
                try
                {
                    if (IsHandleCreated && !IsDisposed)
                    {
                        BeginInvoke((Action)OnRecordingStopped);
                    }
                }
                catch
                {
                }
            }
        }

        private void OnRecordingStopped()
        {
            _modeCombo.Enabled = true;
            _windowCombo.Enabled = true;
            _refreshBtn.Enabled = true;
            _recordBtn.Text = "Record (F9)";
            _recordBtn.BackColor = Color.FromArgb(196, 56, 68);
            if (WindowState == FormWindowState.Minimized)
            {
                WindowState = FormWindowState.Normal;
                Activate();
            }
        }

        private void StopRecordingAsync()
        {
            _stopRequested = true;
            _statusText = "Finalizing MP4...";
        }

        private void StopRecordingBlocking()
        {
            _stopRequested = true;
            if (_recordThread != null && _recordThread.IsAlive)
            {
                _recordThread.Join(5000);
            }
            if (_idlePreviewEngine != null)
            {
                _idlePreviewEngine.Dispose();
                _idlePreviewEngine = null;
            }
        }

        private void TakeSnapshot()
        {
            CaptureMode mode = (CaptureMode)_modeCombo.SelectedIndex;
            WindowChoice target = _windowCombo.SelectedItem as WindowChoice;
            Rectangle region = GetCaptureRegion(target);

            using (ScreenCaptureEngine cap = new ScreenCaptureEngine(mode))
            {
                byte[] pixels;
                int w, h;
                if (cap.CaptureFrame(region, out pixels, out w, out h))
                {
                    string pngPath = Path.Combine(_outputDir, "snapshot_" + DateTime.Now.ToString("yyyyMMdd_HHmmss") + ".png");
                    using (Bitmap bmp = PixelsToBitmap(pixels, w, h))
                    {
                        bmp.Save(pngPath, ImageFormat.Png);
                    }
                    _backendText = cap.ActiveBackend;
                    _statusText = "Saved " + Path.GetFileName(pngPath);
                }
                else
                {
                    _statusText = "Snapshot failed";
                }
            }
        }

        private void PlayLastVideo()
        {
            if (!string.IsNullOrEmpty(_lastVideoPath) && File.Exists(_lastVideoPath))
            {
                Process.Start(new ProcessStartInfo(_lastVideoPath) { UseShellExecute = true });
            }
        }

        private void OpenOutputFolder()
        {
            Directory.CreateDirectory(_outputDir);
            Process.Start(new ProcessStartInfo(_outputDir) { UseShellExecute = true });
        }

        private static Bitmap PixelsToBitmap(byte[] pixels, int width, int height)
        {
            Bitmap bmp = new Bitmap(width, height, PixelFormat.Format32bppRgb);
            BitmapData data = bmp.LockBits(new Rectangle(0, 0, width, height), ImageLockMode.WriteOnly, PixelFormat.Format32bppRgb);
            try
            {
                int rowBytes = width * 4;
                if (data.Stride == rowBytes)
                {
                    Marshal.Copy(pixels, 0, data.Scan0, rowBytes * height);
                }
                else
                {
                    for (int y = 0; y < height; y++)
                    {
                        Marshal.Copy(pixels, y * rowBytes, IntPtr.Add(data.Scan0, y * data.Stride), rowBytes);
                    }
                }
            }
            finally
            {
                bmp.UnlockBits(data);
            }
            return bmp;
        }
    }

    internal static class SelfTest
    {
        public static int Run()
        {
            string tempDir = Path.Combine(Path.GetTempPath(), "streamproof_selftest_" + Guid.NewGuid().ToString("N"));
            Directory.CreateDirectory(tempDir);
            string mp4Path = Path.Combine(tempDir, "test.mp4");
            string pngDxgiPath = Path.Combine(tempDir, "dxgi.png");
            string pngGdiPath = Path.Combine(tempDir, "gdi.png");

            try
            {
                Rectangle bounds = Screen.PrimaryScreen.Bounds;
                int w = Math.Min(bounds.Width, 640) & ~1;
                int h = Math.Min(bounds.Height, 360) & ~1;
                Rectangle testRegion = new Rectangle(bounds.X, bounds.Y, w, h);

                byte[] dxgiPixels;
                int dxgiW, dxgiH;
                using (ScreenCaptureEngine dxgiCap = new ScreenCaptureEngine(CaptureMode.DxgiDuplication))
                {
                    bool okDxgi = false;
                    dxgiPixels = null; dxgiW = 0; dxgiH = 0;
                    for (int attempt = 0; attempt < 5; attempt++)
                    {
                        okDxgi = dxgiCap.CaptureFrame(testRegion, out dxgiPixels, out dxgiW, out dxgiH);
                        if (okDxgi) break;
                        Thread.Sleep(50);
                    }
                    Console.WriteLine("DXGI=" + okDxgi + " diag=" + DxgiDuplicator.LastDiag + " backend=" + dxgiCap.ActiveBackend + " size=" + dxgiW + "x" + dxgiH);
                }

                byte[] gdiPixels;
                int gdiW, gdiH;
                using (ScreenCaptureEngine gdiCap = new ScreenCaptureEngine(CaptureMode.GdiBitBlt))
                {
                    bool okGdi = gdiCap.CaptureFrame(testRegion, out gdiPixels, out gdiW, out gdiH);
                    Console.WriteLine("Session=" + Process.GetCurrentProcess().SessionId
                        + " GDI=" + okGdi + " err=" + ScreenCaptureEngine.LastGdiError
                        + " backend=" + gdiCap.ActiveBackend + " size=" + gdiW + "x" + gdiH);
                }

                byte[] testPixels = dxgiPixels ?? gdiPixels;
                bool synthetic = testPixels == null;
                if (synthetic)
                {
                    testPixels = new byte[w * h * 4];
                    for (int row = 0; row < h; row++)
                    {
                        bool topHalf = row < h / 2;
                        for (int col = 0; col < w; col++)
                        {
                            int idx = (row * w + col) * 4;
                            testPixels[idx] = (byte)(topHalf ? 20 : 230);     // B
                            testPixels[idx + 1] = (byte)(topHalf ? 230 : 20); // G
                            testPixels[idx + 2] = 20;                         // R
                            testPixels[idx + 3] = 255;                        // A
                        }
                    }
                }
                using (Mp4VideoWriter writer = new Mp4VideoWriter(mp4Path, w, h, 30, 4000000))
                {
                    for (int i = 0; i < 15; i++)
                    {
                        writer.WriteFrame(testPixels, i * (10000000L / 30));
                    }
                    writer.Complete();
                }

                long len = new FileInfo(mp4Path).Length;
                Console.WriteLine("MP4=" + (len > 1000) + " bytes=" + len);
                return len > 1000 ? 0 : 4;
            }
            catch (Exception ex)
            {
                Console.WriteLine("SELFTEST_ERROR=" + ex);
                return 1;
            }
            finally
            {
                try { Directory.Delete(tempDir, true); } catch { }
            }
        }
    }

    internal static class ComHelper
    {
        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate int QueryInterfaceDelegate(IntPtr self, ref Guid iid, out IntPtr obj);

        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate uint ReleaseDelegate(IntPtr self);

        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate int EnumOutputsDelegate(IntPtr self, uint index, out IntPtr output);

        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate int GetOutputDescDelegate(IntPtr self, out NativeMethods.DXGI_OUTPUT_DESC desc);

        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate int DuplicateOutputDelegate(IntPtr self, IntPtr device, out IntPtr dupl);

        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate int AcquireNextFrameDelegate(IntPtr self, uint timeoutMs, out NativeMethods.DXGI_OUTDUPL_FRAME_INFO info, out IntPtr resource);

        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate int VoidIntDelegate(IntPtr self);

        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate int CreateTexture2DDelegate(IntPtr self, ref NativeMethods.D3D11_TEXTURE2D_DESC desc, IntPtr initialData, out IntPtr tex);

        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate void GetTexture2DDescDelegate(IntPtr self, out NativeMethods.D3D11_TEXTURE2D_DESC desc);

        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate void CopyResourceDelegate(IntPtr self, IntPtr dst, IntPtr src);

        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate int MapDelegate(IntPtr self, IntPtr resource, uint subresource, uint mapType, uint mapFlags, out NativeMethods.D3D11_MAPPED_SUBRESOURCE mapped);

        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate void UnmapDelegate(IntPtr self, IntPtr resource, uint subresource);

        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate int SetUint32Delegate(IntPtr self, ref Guid key, uint val);

        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate int SetUint64Delegate(IntPtr self, ref Guid key, ulong val);

        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate int SetGuidDelegate(IntPtr self, ref Guid key, ref Guid val);

        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate int AddStreamDelegate(IntPtr self, IntPtr mediaType, out uint streamIndex);

        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate int SetInputMediaTypeDelegate(IntPtr self, uint streamIndex, IntPtr mediaType, IntPtr encodingParams);

        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate int WriteSampleDelegate(IntPtr self, uint streamIndex, IntPtr sample);

        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate int LockBufferDelegate(IntPtr self, out IntPtr buffer, out uint maxLen, out uint curLen);

        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate int SetCurrentLengthDelegate(IntPtr self, uint len);

        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate int AddBufferDelegate(IntPtr self, IntPtr buffer);

        [UnmanagedFunctionPointer(CallingConvention.StdCall)]
        private delegate int SetSampleTimeDelegate(IntPtr self, long hns);

        private static T Slot<T>(IntPtr comObj, int index) where T : class
        {
            IntPtr vtable = Marshal.ReadIntPtr(comObj);
            IntPtr fnPtr = Marshal.ReadIntPtr(vtable, index * IntPtr.Size);
            return Marshal.GetDelegateForFunctionPointer(fnPtr, typeof(T)) as T;
        }

        public static int QueryInterface(IntPtr self, ref Guid iid, out IntPtr obj) { return Slot<QueryInterfaceDelegate>(self, 0)(self, ref iid, out obj); }
        public static uint AddRef(IntPtr self) { return Slot<ReleaseDelegate>(self, 1)(self); }
        public static uint Release(IntPtr self) { return Slot<ReleaseDelegate>(self, 2)(self); }
        public static int EnumAdapters(IntPtr self, uint idx, out IntPtr adapter) { return Slot<EnumOutputsDelegate>(self, 7)(self, idx, out adapter); }
        public static int EnumOutputs(IntPtr self, uint idx, out IntPtr output) { return Slot<EnumOutputsDelegate>(self, 7)(self, idx, out output); }
        public static int GetOutputDesc(IntPtr self, out NativeMethods.DXGI_OUTPUT_DESC desc) { return Slot<GetOutputDescDelegate>(self, 7)(self, out desc); }
        public static int DuplicateOutput(IntPtr self, IntPtr device, out IntPtr dupl) { return Slot<DuplicateOutputDelegate>(self, 22)(self, device, out dupl); }
        public static int AcquireNextFrame(IntPtr self, uint timeout, out NativeMethods.DXGI_OUTDUPL_FRAME_INFO info, out IntPtr res) { return Slot<AcquireNextFrameDelegate>(self, 8)(self, timeout, out info, out res); }
        public static int ReleaseFrame(IntPtr self) { return Slot<VoidIntDelegate>(self, 14)(self); }
        public static int CreateTexture2D(IntPtr self, ref NativeMethods.D3D11_TEXTURE2D_DESC desc, out IntPtr tex) { return Slot<CreateTexture2DDelegate>(self, 5)(self, ref desc, IntPtr.Zero, out tex); }
        public static void GetTexture2DDesc(IntPtr self, out NativeMethods.D3D11_TEXTURE2D_DESC desc) { Slot<GetTexture2DDescDelegate>(self, 10)(self, out desc); }
        public static void CopyResource(IntPtr self, IntPtr dst, IntPtr src) { Slot<CopyResourceDelegate>(self, 47)(self, dst, src); }
        public static int Map(IntPtr self, IntPtr res, uint sub, uint type, uint flags, out NativeMethods.D3D11_MAPPED_SUBRESOURCE mapped) { return Slot<MapDelegate>(self, 14)(self, res, sub, type, flags, out mapped); }
        public static void Unmap(IntPtr self, IntPtr res, uint sub) { Slot<UnmapDelegate>(self, 15)(self, res, sub); }

        public static int SetUINT32(IntPtr self, ref Guid key, uint val) { return Slot<SetUint32Delegate>(self, 21)(self, ref key, val); }
        public static int SetUINT64(IntPtr self, ref Guid key, ulong val) { return Slot<SetUint64Delegate>(self, 22)(self, ref key, val); }
        public static int SetGUID(IntPtr self, ref Guid key, ref Guid val) { return Slot<SetGuidDelegate>(self, 24)(self, ref key, ref val); }
        public static int AddStream(IntPtr self, IntPtr mediaType, out uint streamIndex) { return Slot<AddStreamDelegate>(self, 3)(self, mediaType, out streamIndex); }
        public static int SetInputMediaType(IntPtr self, uint streamIndex, IntPtr mediaType, IntPtr encodingParams) { return Slot<SetInputMediaTypeDelegate>(self, 4)(self, streamIndex, mediaType, encodingParams); }
        public static int BeginWriting(IntPtr self) { return Slot<VoidIntDelegate>(self, 5)(self); }
        public static int WriteSample(IntPtr self, uint streamIndex, IntPtr sample) { return Slot<WriteSampleDelegate>(self, 6)(self, streamIndex, sample); }
        public static int FinalizeWriter(IntPtr self) { return Slot<VoidIntDelegate>(self, 11)(self); }
        public static int LockBuffer(IntPtr self, out IntPtr ptr, out uint maxLen, out uint curLen) { return Slot<LockBufferDelegate>(self, 3)(self, out ptr, out maxLen, out curLen); }
        public static int UnlockBuffer(IntPtr self) { return Slot<VoidIntDelegate>(self, 4)(self); }
        public static int SetCurrentLength(IntPtr self, uint len) { return Slot<SetCurrentLengthDelegate>(self, 6)(self, len); }
        public static int AddBuffer(IntPtr self, IntPtr buf) { return Slot<AddBufferDelegate>(self, 42)(self, buf); }
        public static int SetSampleTime(IntPtr self, long hns) { return Slot<SetSampleTimeDelegate>(self, 36)(self, hns); }
        public static int SetSampleDuration(IntPtr self, long hns) { return Slot<SetSampleTimeDelegate>(self, 38)(self, hns); }
    }

    internal static class NativeMethods
    {
        public const int SRCCOPY = 0x00CC0020;
        public const int CAPTUREBLT = 0x40000000;
        public const uint BI_RGB = 0;

        public delegate bool EnumWindowsProc(IntPtr hwnd, IntPtr lParam);

        [StructLayout(LayoutKind.Sequential)]
        public struct RECT { public int Left, Top, Right, Bottom; }

        [StructLayout(LayoutKind.Sequential)]
        public struct POINT { public int X, Y; }

        [StructLayout(LayoutKind.Sequential)]
        public struct BITMAPINFOHEADER
        {
            public int biSize;
            public int biWidth;
            public int biHeight;
            public short biPlanes;
            public short biBitCount;
            public uint biCompression;
            public uint biSizeImage;
            public int biXPelsPerMeter;
            public int biYPelsPerMeter;
            public uint biClrUsed;
            public uint biClrImportant;
        }

        [StructLayout(LayoutKind.Sequential)]
        public struct BITMAPINFO
        {
            public BITMAPINFOHEADER bmiHeader;
            public uint bmiColors;
        }

        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
        public struct DXGI_OUTPUT_DESC
        {
            [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 32)]
            public string DeviceName;
            public RECT DesktopCoordinates;
            public int AttachedToDesktop;
            public int Rotation;
            public IntPtr Monitor;
        }

        [StructLayout(LayoutKind.Sequential)]
        public struct DXGI_OUTDUPL_POINTER_POSITION
        {
            public POINT Position;
            public int Visible;
        }

        [StructLayout(LayoutKind.Sequential)]
        public struct DXGI_OUTDUPL_FRAME_INFO
        {
            public long LastPresentTime;
            public long LastMouseUpdateTime;
            public uint AccumulatedFrames;
            public int RectsCoalesced;
            public int ProtectedContentMaskedOut;
            public DXGI_OUTDUPL_POINTER_POSITION PointerPosition;
            public uint TotalMetadataBufferSize;
            public uint PointerShapeBufferSize;
        }

        [StructLayout(LayoutKind.Sequential)]
        public struct D3D11_TEXTURE2D_DESC
        {
            public uint Width;
            public uint Height;
            public uint MipLevels;
            public uint ArraySize;
            public uint Format;
            public uint SampleCount;
            public uint SampleQuality;
            public uint Usage;
            public uint BindFlags;
            public uint CPUAccessFlags;
            public uint MiscFlags;
        }

        [StructLayout(LayoutKind.Sequential)]
        public struct D3D11_MAPPED_SUBRESOURCE
        {
            public IntPtr pData;
            public uint RowPitch;
            public uint DepthPitch;
        }

        [DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
        [DllImport("user32.dll")] public static extern IntPtr GetDC(IntPtr hWnd);
        [DllImport("user32.dll")] public static extern int ReleaseDC(IntPtr hWnd, IntPtr hDC);
        [DllImport("user32.dll")] public static extern bool EnumWindows(EnumWindowsProc lpEnumFunc, IntPtr lParam);
        [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr hWnd);
        [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr hWnd);
        [DllImport("user32.dll")] public static extern bool IsWindow(IntPtr hWnd);
        [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern int GetWindowTextLength(IntPtr hWnd);
        [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern int GetWindowText(IntPtr hWnd, StringBuilder lpString, int nMaxCount);
        [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);
        [DllImport("user32.dll")] public static extern bool SetWindowDisplayAffinity(IntPtr hWnd, uint dwAffinity);
        [DllImport("user32.dll")] public static extern bool RegisterHotKey(IntPtr hWnd, int id, uint fsModifiers, uint vk);
        [DllImport("user32.dll")] public static extern bool UnregisterHotKey(IntPtr hWnd, int id);

        [DllImport("dwmapi.dll")] public static extern int DwmGetWindowAttribute(IntPtr hwnd, int dwAttribute, out RECT pvAttribute, int cbAttribute);

        [DllImport("gdi32.dll")] public static extern IntPtr CreateCompatibleDC(IntPtr hdc);
        [DllImport("gdi32.dll")] public static extern bool DeleteDC(IntPtr hdc);
        [DllImport("gdi32.dll")] public static extern IntPtr SelectObject(IntPtr hdc, IntPtr hgdiobj);
        [DllImport("gdi32.dll")] public static extern bool DeleteObject(IntPtr hObject);
        [DllImport("gdi32.dll")] public static extern IntPtr CreateDIBSection(IntPtr hdc, ref BITMAPINFO pbmi, uint iUsage, out IntPtr ppvBits, IntPtr hSection, uint dwOffset);
        [DllImport("gdi32.dll", SetLastError = true)] public static extern bool BitBlt(IntPtr hdcDest, int nXDest, int nYDest, int nWidth, int nHeight, IntPtr hdcSrc, int nXSrc, int nYSrc, int dwRop);

        [DllImport("dxgi.dll")] public static extern int CreateDXGIFactory1(ref Guid riid, out IntPtr ppFactory);

        [DllImport("d3d11.dll")]
        public static extern int D3D11CreateDevice(
            IntPtr pAdapter, int DriverType, IntPtr Software, uint Flags,
            IntPtr pFeatureLevels, uint FeatureLevels, uint SDKVersion,
            out IntPtr ppDevice, out int pFeatureLevel, out IntPtr ppImmediateContext);

        [DllImport("mfplat.dll")] public static extern int MFStartup(uint version, uint dwFlags);
        [DllImport("mfplat.dll")] public static extern int MFShutdown();
        [DllImport("mfplat.dll")] public static extern int MFCreateAttributes(out IntPtr ppMFAttributes, uint cInitialSize);
        [DllImport("mfplat.dll")] public static extern int MFCreateMediaType(out IntPtr ppMFType);
        [DllImport("mfplat.dll")] public static extern int MFCreateSample(out IntPtr ppIMFSample);
        [DllImport("mfplat.dll")] public static extern int MFCreateMemoryBuffer(uint cbMaxLength, out IntPtr ppBuffer);

        [DllImport("mfreadwrite.dll", CharSet = CharSet.Unicode)]
        public static extern int MFCreateSinkWriterFromURL(
            string pwszOutputURL, IntPtr pByteStream, IntPtr pAttributes, out IntPtr ppSinkWriter);
    }
}
