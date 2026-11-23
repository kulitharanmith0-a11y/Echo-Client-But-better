package com.blamejared.clumps.client.modules.impl.render;

import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.render.hand.*;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.utils.strings.Concat;
import java.util.Arrays;
import java.util.List;

public class ViewModel extends Module {
    public List<HandAnimation> animations;
    public static BooleanSetting eatingAnimation;
    public static FloatSetting eatingBob;
    public static ChoiceSetting styles;
    public static FloatSetting strengthMain;
    public static FloatSetting strengthOff;
    public static FloatSetting scaleMain;
    public static FloatSetting scaleOff;
    public static FloatSetting posXMain;
    public static FloatSetting posYMain;
    public static FloatSetting posZMain;
    public static FloatSetting posXOff;
    public static FloatSetting posYOff;
    public static FloatSetting posZOff;
    public static FloatSetting rotXMain;
    public static FloatSetting rotYMain;
    public static FloatSetting rotZMain;
    public static FloatSetting rotXOff;
    public static FloatSetting rotYOff;
    public static FloatSetting rotZOff;

    public ViewModel() {
        super(new ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("View Model"), com.blamejared.clumps.client.init.CatalogText.literal("Modifies hand/item rendering"), com.blamejared.clumps.client.modules.ModuleCategory.RENDER, false));
    }

    @Override
    public void postInitialize() {
        animations = Arrays.asList(
                new DefaultHandAnimation(), new NoneHandAnimation(), new CustomHandAnimation(), new SmoothHandAnimation(), new BounceHandAnimation(), new SlashHandAnimation(), new PunchHandAnimation(), new SwipeHandAnimation(), new Spin360HandAnimation(), new ScaleHandAnimation(), new SpinHandAnimation(), new KatanaHandAnimation(), new SigmaHandAnimation(), new PushHandAnimation(), new StabHandAnimation(), new SliceHandAnimation(), new ExhibitionHandAnimation());
        eatingAnimation.tooltip("Appearance");
        scaleMain.tooltip("Appearance");
        scaleOff.tooltip("Appearance");
    }

    public HandAnimation getSelectedAnimation() {
        for (HandAnimation animation : animations) {
            if (styles.isSelected(animation.displayName())) {
                return animation;
            }
        }
        return animations.get(0);
    }

}
