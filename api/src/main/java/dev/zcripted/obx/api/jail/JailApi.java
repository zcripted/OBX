package dev.zcripted.obx.api.jail;

import java.util.Collection;

/**
 * Public jail surface used across features (the staff admin menu lists jails).
 * Implemented by {@code feature.jail.service.JailService}.
 */
public interface JailApi {

    Collection<Jail> getJails();
}
