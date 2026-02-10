package net.axes.naturalregrowth.mixin;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class NaturalRegrowthMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        // No-op
    }

    @Override
    public String getRefMapperConfig() {
        return null; // Use default
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // --- GUARD: RotProtectionMixin ---
        // We check if the mixin being applied is our RotProtectionMixin.
        // If it is, we ONLY allow it if "dynamictrees" is currently loaded.
        if (mixinClassName.endsWith("RotProtectionMixin")) {
            return LoadingModList.get().getModFileById("dynamictrees") != null;
        }

        // All other mixins (Vanilla compatibility) are always allowed.
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // No-op
    }

    @Override
    public List<String> getMixins() {
        return null; // No extra mixins to add
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No-op
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No-op
    }
}