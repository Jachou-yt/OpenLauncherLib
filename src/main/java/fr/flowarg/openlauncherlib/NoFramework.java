package fr.flowarg.openlauncherlib;

import fr.theshark34.openlauncherlib.configuration.api.json.JSONReader;
import fr.theshark34.openlauncherlib.external.ExternalLaunchProfile;
import fr.theshark34.openlauncherlib.external.ExternalLauncher;
import fr.theshark34.openlauncherlib.minecraft.AuthInfos;
import fr.theshark34.openlauncherlib.minecraft.GameFolder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static class Parameters
    {
        private JSONObject vanilla;
        private JSONObject processing;
    }

    public enum Type
    {
        VM,
        GAME
    }

    public NoFramework(Path gameDir, AuthInfos infos, GameFolder folder)
    {
        this(gameDir, infos, folder, new ArrayList<>(), new ArrayList<>());
    }

    public NoFramework(Path gameDir, AuthInfos infos, GameFolder folder, List<String> additionalArgs, Type type)
    {
        this(gameDir, infos, folder, type == Type.VM ? additionalArgs : new ArrayList<>(), type == Type.GAME ? additionalArgs : new ArrayList<>());
    }

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

    public void launch(String version, String forgeVersion) throws Exception
    {
        final JSONObject vanilla = new JSONReader(Logger.getLogger("OpenLauncherLib"), this.gameDir.resolve(version + ".json")).toJSONObject();
        final JSONObject forge = new JSONReader(Logger.getLogger("OpenLauncherLib"), this.gameDir.resolve(version + "-forge-" + forgeVersion + ".json")).toJSONObject();

        final ExternalLauncher launcher = new ExternalLauncher(new ExternalLaunchProfile(
                forge.getString("mainClass"),
                getClassPath(vanilla, forge),
                getVmArgs(vanilla, forge),
                getArgs(vanilla, forge),
                true,
                "testlauncherupdater",
                this.gameDir
        ));
        launcher.launch().waitFor();
    }

    private List<String> getVmArgs(JSONObject vanilla, JSONObject forge)
    {
        final List<String> result = new ArrayList<>(getVmArgsFor(vanilla, vanilla));
        result.addAll(getVmArgsFor(forge, vanilla));
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

                sb.add(map(arg, parameters));
            }
        }

        return sb;
    }

    private String getClassPath(JSONObject vanilla, JSONObject forge)
    {
        final List<String> cp = new ArrayList<>();

        appendLibraries(cp, vanilla);
        appendLibraries(cp, forge);

        cp.add(this.gameDir.resolve("client.jar").toString());

        return this.toString(cp);
    }

    private void appendLibraries(List<String> sb, JSONObject object)
    {
        object.getJSONArray("libraries").forEach(jsonElement -> {

            final Path path = libraries.resolve(((JSONObject)jsonElement).getJSONObject("downloads").getJSONObject("artifact").getString("path"));
            final String str =  path + File.pathSeparator;
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
        result.addAll(getArgs(forge, parameters));
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
                sb.add(map((String)element, parameters));
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

    public void setAdditionalArgs(List<String> additionalArgs)
    {
        this.additionalArgs = additionalArgs;
    }

    public void setAdditionalVmArgs(List<String> additionalVmArgs)
    {
        this.additionalVmArgs = additionalVmArgs;
    }
}
