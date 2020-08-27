package fr.flowarg.openlauncherlib;

@ModifiedByFlow
public class NewForgeVersionDiscriminator
{
	/** e.g : 32.0.2 */
    private final String forgeVersion;
    /** e.g : 1.15.2 */
    private final String mcVersion;
    /** net.minecraftforge by default */
    private final String forgeGroup;
    /** e.g : 20200625.160719 */
    private final String mcpVersion;

    public NewForgeVersionDiscriminator(String forgeVersion, String mcVersion, String forgeGroup, String mcpVersion)
    {
        this.forgeVersion = forgeVersion;
        this.mcVersion    = mcVersion;
        this.forgeGroup   = forgeGroup;
        this.mcpVersion   = mcpVersion;
    }

    public String getForgeVersion()
    {
        return this.forgeVersion;
    }

    public String getMcVersion()
    {
        return this.mcVersion;
    }

    public String getForgeGroup()
    {
        return this.forgeGroup;
    }

    public String getMcpVersion()
    {
        return this.mcpVersion;
    }
}
