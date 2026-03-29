package com.gilrossi.despesas.historyimport;

import java.util.List;

public record HistoryImportResponse(
	int importedCount,
	List<HistoryImportEntryResponse> entries
) {
}
