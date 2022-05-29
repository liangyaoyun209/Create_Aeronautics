package com.eriksonn.createaeronautics.blocks.analog_clutch;

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.INamedIconOptions;

public enum RotationalGateMode implements INamedIconOptions {
    ALLOW_ROTATION("Redstone signal allows rotation", AllIcons.I_ROTATE_CCW),
    STOP_ROTATION("Redstone signal stops rotation", AllIcons.I_STOP);

    private String name;
    private AllIcons icon;

    RotationalGateMode(String name, AllIcons icon) {
        this.name = name;
        this.icon = icon;
    }

    @Override
    public AllIcons getIcon() {
        return icon;
    }

    @Override
    public String getTranslationKey() {
        return name;
    }

}
