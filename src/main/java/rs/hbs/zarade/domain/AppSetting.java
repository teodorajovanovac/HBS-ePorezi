package rs.hbs.zarade.domain;

/**
 * Jedan red tabele Settings iz data_zarade.mdb.
 *
 * Kolone: IDSettings (autonumber), SettingName, SettingVal, Descrition, Category, MFF.
 * Napomena: kolona za opis se u bazi zove "Descrition".
 */
public class AppSetting {

    private Integer idSettings;
    private String settingName;
    private String settingVal;
    private String description;
    private String category;
    private String mff;

    public AppSetting() {}

    public AppSetting(Integer idSettings, String settingName, String settingVal,
                      String description, String category, String mff) {
        this.idSettings = idSettings;
        this.settingName = settingName;
        this.settingVal = settingVal;
        this.description = description;
        this.category = category;
        this.mff = mff;
    }

    public Integer getIdSettings() { return idSettings; }
    public void setIdSettings(Integer idSettings) { this.idSettings = idSettings; }

    public String getSettingName() { return settingName; }
    public void setSettingName(String settingName) { this.settingName = settingName; }

    public String getSettingVal() { return settingVal; }
    public void setSettingVal(String settingVal) { this.settingVal = settingVal; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getMff() { return mff; }
    public void setMff(String mff) { this.mff = mff; }

    /** Da li bilo koje polje (osim IDSettings) sadrži tekst — bez obzira na velika/mala slova. */
    public boolean matches(String filter) {
        if (filter == null || filter.isBlank()) return true;
        String f = filter.toLowerCase();
        for (String v : new String[]{settingName, settingVal, description, category, mff}) {
            if (v != null && v.toLowerCase().contains(f)) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return settingName != null ? settingName : "(bez imena)";
    }
}
