package com.mcalvaro.mscatering.application.consolidatedcalendar.GetConsolidatedCalendarByDate;

import an.awesome.pipelinr.Command;

/**
 * Handler for {@link GetConsolidatedCalendarByDateQuery}.
 * <p>
 * Delegates to the {@link IConsolidatedCalendarQueryService} port
 * to fetch the read model directly from the database.
 */
public class GetConsolidatedCalendarByDateQueryHandler
        implements Command.Handler<GetConsolidatedCalendarByDateQuery, ConsolidatedCalendarDto> {

    private final IConsolidatedCalendarQueryService queryService;

    public GetConsolidatedCalendarByDateQueryHandler(IConsolidatedCalendarQueryService queryService) {
        this.queryService = queryService;
    }

    @Override
    public ConsolidatedCalendarDto handle(GetConsolidatedCalendarByDateQuery query) {
        return queryService.getByDate(query.date())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Consolidated calendar not found for date: " + query.date()));
    }
}
