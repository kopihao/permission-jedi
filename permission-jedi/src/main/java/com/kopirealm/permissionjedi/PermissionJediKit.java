package com.kopirealm.permissionjedi;

import java.io.Serializable;

public class PermissionJediKit implements Serializable {

    public static final String EXTRA_KEY = "EXTRA_PERMISSION_JEDI_KIT";

    public final static PermissionJediKit renounce(Serializable input) {
        if (input instanceof PermissionJediKit) {
            return PermissionJediKit.class.cast(input);
        }
        return new PermissionJediKit();
    }

    private String action = "";
    private String[] permissions = new String[0];

    protected final void setAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public void setPermissions(String[] permissions) {
        this.permissions = permissions;
    }

    public String[] getPermissions() {
        return permissions;
    }

    public Serializable serialized() {
        return this;
    }

}
