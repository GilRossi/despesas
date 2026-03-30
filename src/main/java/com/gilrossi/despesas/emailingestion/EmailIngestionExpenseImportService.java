package com.gilrossi.despesas.emailingestion;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.CategoryRepository;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.catalog.subcategory.SubcategoryRepository;
import com.gilrossi.despesas.expense.CreateExpenseRequest;
import com.gilrossi.despesas.expense.ExpenseContext;
import com.gilrossi.despesas.expense.ExpenseResponse;
import com.gilrossi.despesas.expense.ExpenseService;

@Service
public class EmailIngestionExpenseImportService {

	private final CategoryRepository categoryRepository;
	private final SubcategoryRepository subcategoryRepository;
	private final ExpenseService expenseService;

	public EmailIngestionExpenseImportService(
		CategoryRepository categoryRepository,
		SubcategoryRepository subcategoryRepository,
		ExpenseService expenseService
	) {
		this.categoryRepository = categoryRepository;
		this.subcategoryRepository = subcategoryRepository;
		this.expenseService = expenseService;
	}

	public ExpenseResponse importExpense(Long householdId, String sourceAccount, ProcessEmailIngestionCommand command) {
		Category category = categoryRepository.findByNameIgnoreCase(householdId, command.suggestedCategoryName())
			.orElseThrow(() -> new EmailIngestionImportReviewException(
				EmailIngestionDecisionReason.CATEGORY_NOT_FOUND,
				"Suggested category could not be resolved for auto import"
			));
		Subcategory subcategory = resolveSubcategory(householdId, category, command.suggestedSubcategoryName());
		return expenseService.criarParaHousehold(householdId, new CreateExpenseRequest(
			buildDescription(command),
			command.totalAmount(),
			resolveOccurredOn(command),
			resolveDueDate(command),
			resolveContext(command, category.getName()),
			category.getId(),
			subcategory.getId(),
			null,
			buildNotes(sourceAccount, command)
		));
	}

	private Subcategory resolveSubcategory(Long householdId, Category category, String suggestedSubcategoryName) {
		List<Subcategory> categorySubcategories = subcategoryRepository.findActiveByHouseholdId(householdId).stream()
			.filter(item -> item.getCategoryId().equals(category.getId()))
			.toList();
		if (StringUtils.hasText(suggestedSubcategoryName)) {
			return categorySubcategories.stream()
				.filter(item -> item.getName().equalsIgnoreCase(suggestedSubcategoryName.trim()))
				.findFirst()
				.orElseThrow(() -> new EmailIngestionImportReviewException(
					EmailIngestionDecisionReason.SUBCATEGORY_NOT_FOUND,
					"Suggested subcategory could not be resolved for auto import"
				));
		}
		if (categorySubcategories.size() == 1) {
			return categorySubcategories.getFirst();
		}
		throw new EmailIngestionImportReviewException(
			EmailIngestionDecisionReason.SUBCATEGORY_REQUIRED,
			"Auto import requires a resolvable subcategory"
		);
	}

	private ExpenseContext resolveContext(ProcessEmailIngestionCommand command, String categoryName) {
		String normalizedCategory = normalize(categoryName);
		String normalizedMerchant = normalize(command.merchantOrPayee());
		String normalizedSubject = normalize(command.subject());
		if (command.classification() == EmailIngestionClassification.RECURRING_BILL) {
			return ExpenseContext.CASA;
		}
		if (normalizedCategory.contains("pet") || normalizedMerchant.contains("pet") || normalizedMerchant.contains("cobasi")
			|| normalizedSubject.contains("pet")) {
			return ExpenseContext.PETS;
		}
		if (normalizedCategory.contains("casa") || normalizedMerchant.contains("internet") || normalizedMerchant.contains("energia")
			|| normalizedMerchant.contains("luz") || normalizedMerchant.contains("gas") || normalizedMerchant.contains("celular")) {
			return ExpenseContext.CASA;
		}
		if (normalizedMerchant.contains("uber") || normalizedSubject.contains("uber")) {
			return ExpenseContext.UBER;
		}
		if (normalizedCategory.contains("veic") || normalizedMerchant.contains("combust") || normalizedMerchant.contains("posto")) {
			return ExpenseContext.VEICULO;
		}
		return ExpenseContext.GERAL;
	}

	private LocalDate resolveDueDate(ProcessEmailIngestionCommand command) {
		if (command.dueDate() != null) {
			return command.dueDate();
		}
		return null;
	}

	private LocalDate resolveOccurredOn(ProcessEmailIngestionCommand command) {
		if (command.occurredOn() != null) {
			return command.occurredOn();
		}
		if (command.dueDate() != null) {
			return command.dueDate();
		}
		return command.receivedAt().toLocalDate();
	}

	private String buildDescription(ProcessEmailIngestionCommand command) {
		if (StringUtils.hasText(command.merchantOrPayee())) {
			return limit(command.merchantOrPayee().trim(), 140);
		}
		if (command.items() != null && command.items().size() == 1 && StringUtils.hasText(command.items().getFirst().description())) {
			return limit(command.items().getFirst().description().trim(), 140);
		}
		if (StringUtils.hasText(command.summary())) {
			return limit(command.summary().trim(), 140);
		}
		return limit(command.subject().trim(), 140);
	}

	private String buildNotes(String sourceAccount, ProcessEmailIngestionCommand command) {
		String notes = "Importado por e-mail de " + sourceAccount + " - " + command.externalMessageId();
		return limit(notes, 255);
	}

	private String limit(String value, int maxLength) {
		if (value == null) {
			return null;
		}
		return value.length() <= maxLength ? value : value.substring(0, maxLength);
	}

	private String normalize(String value) {
		return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
	}
}
