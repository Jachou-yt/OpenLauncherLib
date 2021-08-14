package fr.flowarg.openlauncherlib;

import fr.theshark34.openlauncherlib.configuration.api.json.JSONReader;
import fr.theshark34.openlauncherlib.external.ExternalLaunchProfile;
import fr.theshark34.openlauncherlib.external.ExternalLauncher;
import fr.theshark34.openlauncherlib.minecraft.AuthInfos;
import fr.theshark34.openlauncherlib.minecraft.GameFolder;
import fr.theshark34.openlauncherlib.util.LogUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

public class NoFramework
{
    private final Map<String, Function<Parameters, String>> keyValue = new HashMap<>();
    private final Path gameDir;
    private final Path libraries;
    private final String clientJar;
    private List<String> additionalVmArgs;
    private List<String> additionalArgs;
    private String customVanillaJsonFileName = "";
    private String customForgeJsonFileName = "";
    private String serverName = "";
    private SafeConsumer<ExternalLauncher> lastCallback;

    private static class Parameters
    {
        private JSONObject vanilla;
        private JSONObject processing;
    }

    public enum Type {
        VM,
        GAME
    }

    public interface SafeConsumer<T> {

        void accept(T t) throws Exception;

        default SafeConsumer<T> andThen(SafeConsumer<? super T> after)
        {
            Objects.requireNonNull(after);
            return t -> { this.accept(t); after.accept(t); };
        }
    }

    /**
     * Construct a new NoFramework object.
     * @param gameDir the path of the game directory.
     * @param infos auth information.
     * @param folder the folders' name.
     */
    public NoFramework(Path gameDir, AuthInfos infos, GameFolder folder)
    {
        this(gameDir, infos, folder, new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Construct a new NoFramework object.
     * @param gameDir the path of the game directory.
     * @param infos auth information.
     * @param folder the folders' name.
     * @param additionalArgs some additional arguments.
     * @param type the type of arguments to add. Can be GAME or VM.
     */
    public NoFramework(Path gameDir, AuthInfos infos, GameFolder folder, List<String> additionalArgs, Type type)
    {
        this(gameDir, infos, folder, type == Type.VM ? additionalArgs : new ArrayList<>(), type == Type.GAME ? additionalArgs : new ArrayList<>());
    }

    /**
     * Construct a new NoFramework object.
     * @param gameDir the path of the game directory.
     * @param infos auth information.
     * @param folder the folders' name.
     * @param additionalVmArgs some additional arguments.
     * @param additionalArgs some additional VM arguments.
     */
    public NoFramework(Path gameDir, AuthInfos infos, GameFolder folder, List<String> additionalVmArgs, List<String> additionalArgs)
    {
        this.gameDir = gameDir;
        this.libraries = this.gameDir.resolve(folder.getLibsFolder());
        this.clientJar = folder.getMainJar();
        this.additionalVmArgs = additionalVmArgs;
        this.additionalArgs = additionalArgs;

        this.keyValue.put("${library_directory}", parameters -> this.libraries.toString());
        this.keyValue.put("${classpath_separator}", parameters -> File.pathSeparator);
        this.keyValue.put("${auth_player_name}", parameters -> infos.getUsername());
        this.keyValue.put("${version_name}", parameters -> parameters.processing.getString("id"));
        this.keyValue.put("${game_directory}", parameters -> this.gameDir.toString());
        this.keyValue.put("${assets_root}", parameters -> this.gameDir.resolve(folder.getAssetsFolder()).toString());
        this.keyValue.put("${assets_index_name}", parameters -> parameters.vanilla.getJSONObject("assetIndex").getString("id"));
        this.keyValue.put("${auth_uuid}", parameters -> infos.getUuid());
        this.keyValue.put("${auth_access_token}", parameters -> infos.getAccessToken());
        this.keyValue.put("${user_type}", parameters -> "mojang");
        this.keyValue.put("${version_type}", parameters -> "release");
        this.keyValue.put("${natives_directory}", parameters -> this.gameDir.resolve(folder.getNativesFolder()).toString());
    }

    /**
     * Launch the game for the specified versions.
     * @param version Minecraft version (like 1.17.1)
     * @param forgeVersion Forge version (like 37.0.33), do NOT pass a version like 1.17.1-37.0.33!
     * @return the launched process
     * @throws Exception throws an exception if an error has occurred.
     */
    public Process launch(String version, String forgeVersion) throws Exception
    {
        final Logger logger = Logger.getLogger("OpenLauncherLib");
        final Path vanillaJson = this.customVanillaJsonFileName.equals("") ? this.gameDir.resolve(version + ".json") : this.gameDir.resolve(this.customVanillaJsonFileName);
        final JSONObject vanilla = new JSONReader(logger, vanillaJson).toJSONObject();
        final Path forgeJson = this.customForgeJsonFileName.equals("") ? this.gameDir.resolve(version + "-forge-" + forgeVersion + ".json") : this.gameDir.resolve(this.customForgeJsonFileName);
        final JSONObject forge = new JSONReader(logger, forgeJson).toJSONObject();

        LogUtil.info("no-framework");

        final ExternalLauncher launcher = new ExternalLauncher(new ExternalLaunchProfile(
                forge.getString("mainClass"),
                this.getClassPath(vanilla, forge),
                this.getVmArgs(vanilla, forge),
                this.getArgs(vanilla, forge),
                true,
                this.serverName.equals("") ? "Minecraft " + version : this.serverName,
                this.gameDir
        ));

        if(this.lastCallback != null)
            this.lastCallback.accept(launcher);

        return launcher.launch();
    }

    private List<String> getVmArgs(JSONObject vanilla, JSONObject forge)
    {
        final List<String> result = new ArrayList<>(this.getVmArgsFor(vanilla, vanilla));
        result.addAll(this.getVmArgsFor(forge, vanilla));
        result.addAll(this.additionalVmArgs);
        return result;
    }

    private List<String> getVmArgsFor(JSONObject object, JSONObject vanilla)
    {
        final Parameters parameters = new Parameters();
        parameters.vanilla = vanilla;
        parameters.processing = object;

        final List<String> sb = new ArrayList<>();

        final JSONArray array = object.getJSONObject("arguments").getJSONArray("jvm");
        for (Object element : array)
        {
            if(element instanceof String)
            {
                final String arg = (String)element;

                if(arg.contains("minecraft.launcher") || arg.contains("${classpath}") || arg.equals("-cp")) continue;

                sb.add(this.map(arg, parameters));
            }
        }

        return sb;
    }

    private String getClassPath(JSONObject vanilla, JSONObject forge)
    {
        final List<String> cp = new ArrayList<>();

        this.appendLibraries(cp, forge);
        this.appendLibraries(cp, vanilla);

        cp.add(this.gameDir.resolve("client.jar").toString());

        return this.toString(cp);
    }

    private void appendLibraries(List<String> sb, JSONObject object)
    {
        object.getJSONArray("libraries").forEach(jsonElement -> {

            final Path path = this.libraries.resolve(((JSONObject)jsonElement).getJSONObject("downloads").getJSONObject("artifact").getString("path"));
            final String str = path + File.pathSeparator;
            if(!sb.contains(str) && Files.exists(path))
                sb.add(str);
        });
    }

    private List<String> getArgs(JSONObject vanilla, JSONObject forge)
    {
        final Parameters parameters = new Parameters();
        parameters.vanilla = vanilla;
        parameters.processing = forge;

        final List<String> result = new ArrayList<>(getArgs(vanilla, parameters));
        result.addAll(this.getArgs(forge, parameters));
        result.addAll(this.additionalArgs);
        return result;
    }

    private List<String> getArgs(JSONObject object, Parameters parameters)
    {
        final JSONArray array = object.getJSONObject("arguments").getJSONArray("game");

        final List<String> sb = new ArrayList<>();

        for (Object element : array)
        {
            if(element instanceof String)
                sb.add(this.map((String)element, parameters));
        }

        return sb;
    }

    private String map(String str, Parameters parameters)
    {
        if(str.contains("${version_name}.jar")) return str.replace("${version_name}.jar", this.clientJar);
        String result = str;
        for(Map.Entry<String, Function<Parameters, String>> entry : keyValue.entrySet())
            result = result.replace(entry.getKey(), entry.getValue().apply(parameters));

        return result;
    }

    private String toString(List<String> stringList)
    {
        final StringBuilder sb = new StringBuilder();
        stringList.forEach(sb::append);
        return sb.toString();
    }

    public List<String> getAdditionalArgs()
    {
        return this.additionalArgs;
    }

    public List<String> getAdditionalVmArgs()
    {
        return this.additionalVmArgs;
    }

    public String getCustomVanillaJsonFileName()
    {
        return this.customVanillaJsonFileName;
    }

    public String getCustomForgeJsonFileName()
    {
        return this.customForgeJsonFileName;
    }

    public String getServerName()
    {
        return this.serverName;
    }

    public SafeConsumer<ExternalLauncher> getLastCallback()
    {
        return this.lastCallback;
    }

    public void setAdditionalArgs(List<String> additionalArgs)
    {
        this.additionalArgs = additionalArgs;
    }

    public void setAdditionalVmArgs(List<String> additionalVmArgs)
    {
        this.additionalVmArgs = additionalVmArgs;
    }

    public void setCustomVanillaJsonFileName(String customVanillaJsonFileName)
    {
        this.customVanillaJsonFileName = customVanillaJsonFileName;
    }

    public void setCustomForgeJsonFileName(String customForgeJsonFileName)
    {
        this.customForgeJsonFileName = customForgeJsonFileName;
    }

    public void setServerName(String serverName)
    {
        this.serverName = serverName;
    }

    public void setLastCallback(SafeConsumer<ExternalLauncher> lastCallback)
    {
        this.lastCallback = lastCallback;
    }
}
