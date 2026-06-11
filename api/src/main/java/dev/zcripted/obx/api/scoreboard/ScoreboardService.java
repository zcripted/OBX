package dev.zcripted.obx.api.scoreboard;

import java.util.List;

/** Public sidebar-scoreboard configuration API. Implemented by {@code feature.scoreboard.service.ScoreboardServiceImpl}. */
public interface ScoreboardService {

    void load();

    void reload();

    boolean isEnabled();

    int getRefreshIntervalTicks();

    String getTitle();

    String getServerIp();

    String getServerWebsite();

    List<String> getLines();
}