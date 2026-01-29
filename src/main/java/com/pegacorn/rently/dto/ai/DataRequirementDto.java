package com.pegacorn.rently.dto.ai;

import java.util.List;

/**
 * DTO representing what data the AI needs to answer a user query.
 * This is returned by the first AI pass to determine what to fetch.
 */
public record DataRequirementDto(
    // What type of query is this?
    QueryType queryType,

    // Does AI need house data?
    boolean needHouses,

    // Does AI need room data? If so, for which house (null = all)?
    boolean needRooms,
    String houseNameFilter,

    // Does AI need contract data?
    boolean needContracts,
    String roomCodeFilter,

    // Does AI need invoice data? For which period?
    boolean needInvoices,
    Integer invoiceMonth,
    Integer invoiceYear,

    // Does AI need expense data? For which period?
    boolean needExpenses,
    Integer expenseMonth,
    Integer expenseYear,

    // Does AI need meter readings?
    boolean needMeterReadings,
    String meterRoomCode,
    String meterPeriod,

    // Is this a simple question that needs no data?
    boolean isSimpleQuestion,

    // Is this an action request?
    boolean isActionRequest,
    String actionType,

    // Is this question unrelated to the system?
    boolean isOffTopic,

    // Original user intent summary
    String intentSummary
) {
    public enum QueryType {
        STATISTICS,      // User wants stats/reports
        LIST_DATA,       // User wants to see a list of items
        SINGLE_ITEM,     // User asking about specific item
        ACTION_REQUEST,  // User wants to perform an action
        HELP_QUESTION,   // User asking how to do something
        GENERAL_CHAT     // General conversation
    }

    // Builder for convenience
    public static DataRequirementDto simpleQuestion(String intent) {
        return new DataRequirementDto(
            QueryType.GENERAL_CHAT, false, false, null, false, null,
            false, null, null, false, null, null, false, null, null,
            true, false, null, false, intent
        );
    }

    public static DataRequirementDto helpQuestion(String intent) {
        return new DataRequirementDto(
            QueryType.HELP_QUESTION, false, false, null, false, null,
            false, null, null, false, null, null, false, null, null,
            true, false, null, false, intent
        );
    }
}
