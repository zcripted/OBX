package dev.zcripted.obx.api.tablist;

import java.util.List;

/** Public tablist-configuration API. Implemented by {@code feature.tablist.service.TablistServiceImpl}. */
public interface TablistService {

    void load();

    void reload();

    boolean isEnabled();

    int getRefreshIntervalTicks();

    List<String> getHeaderLines();

    List<String> getFooterLines();

    String getPlayerFormat();

    boolean isStaffGroupingEnabled();

    String getStaffPlayerFormat();
}