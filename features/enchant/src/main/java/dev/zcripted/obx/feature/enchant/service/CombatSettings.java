package dev.zcripted.obx.feature.enchant.service;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

/**
 * Immutable view of the {@code combat_global} block in {@code enchants/config.yml}
 * — the presentation and safety toggles shared by every Combat-category enchant
 * (holographic FX master switches, particle intensity, sound volume, action-bar
 * feedback, and the per-player hologram cap). Rebuilt on each module reload.
 */
public final class CombatSettings {

    private final boolean damageNumbers;
    private final boolean killBanners;
    private final boolean actionBarFeedback;
    private final String particleIntensity;
    private final double soundVolumeMultiplier;
    private final int maxHologramsPerPlayer;
    private final boolean bloodletterGravestone;
    private final int bloodletterGravestoneSeconds;

    public CombatSettings(ConfigurationSection section) {
        this.damageNumbers = section == null || section.getBoolean("damage_numbers", true);
        this.killBanners = section == null || section.getBoolean("kill_banners", true);
        this.actionBarFeedback = section == null || section.getBoolean("action_bar_feedback", true);
        this.particleIntensity = section == null ? "full" : section.getString("particle_intensity", "full");
        this.soundVolumeMultiplier = section == null ? 1.0 : section.getDouble("sound_volume_multiplier", 1.0);
        this.maxHologramsPerPlayer = section == null ? 12 : section.getInt("max_holograms_per_player", 12);
        this.bloodletterGravestone = section == null || section.getBoolean("bloodletter_gravestone", true);
        this.bloodletterGravestoneSeconds = section == null ? 35 : section.getInt("bloodletter_gravestone_seconds", 35);
    }

    /** Whether a Bloodletter bleed-out death drops a private gravestone marker for the victim. */
    public boolean bloodletterGravestone() {
        return bloodletterGravestone;
    }

    /** How long (seconds) the Bloodletter gravestone marker lasts before vanishing. */
    public int bloodletterGravestoneSeconds() {
        return Math.max(1, bloodletterGravestoneSeconds);
    }

    public boolean damageNumbers() {
        return damageNumbers;
    }

    public boolean killBanners() {
        return killBanners;
    }

    public boolean actionBarFeedback() {
        return actionBarFeedback;
    }

    public boolean particlesOff() {
        return "off".equalsIgnoreCase(particleIntensity);
    }

    public boolean particlesReduced() {
        return "reduced".equalsIgnoreCase(particleIntensity);
    }

    /** Clamped sound-volume multiplier (0.0 silences combat sounds). */
    public float soundVolume() {
        return (float) Math.max(0.0, soundVolumeMultiplier);
    }

    public int maxHologramsPerPlayer() {
        return Math.max(1, maxHologramsPerPlayer);
    }

    /** Lowercased intensity token, for diagnostics. */
    public String particleIntensity() {
        return particleIntensity == null ? "full" : particleIntensity.toLowerCase(Locale.ENGLISH);
    }
}