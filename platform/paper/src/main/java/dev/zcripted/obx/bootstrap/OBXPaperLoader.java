package dev.zcripted.obx.bootstrap;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

/**
 * Paper-native library loader (referenced by paper-plugin.yml's {@code loader:}).
 * paper-plugin.yml does not honor plugin.yml's simple {@code libraries:} list, so
 * the runtime SQLite driver is resolved here via Paper's Maven library loader.
 */
public class OBXPaperLoader implements PluginLoader {

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        resolver.addRepository(new RemoteRepository.Builder(
                "central", "default", "https://repo1.maven.org/maven2/").build());
        resolver.addDependency(new Dependency(
                new DefaultArtifact("org.xerial:sqlite-jdbc:3.45.3.0"), null));
        classpathBuilder.addLibrary(resolver);
    }
}
