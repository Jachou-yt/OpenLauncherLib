package fr.flowarg.openlauncherlib;

@ModifiedByFlow
public class NewForgeVersionDiscriminator
{
    private final String forgeVersion;
    private final String mcVersion;
    private final String forgeGroup;
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
