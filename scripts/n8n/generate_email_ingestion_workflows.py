from __future__ import annotations

import json
import textwrap
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
OUTPUT_DIR = ROOT / "n8n" / "workflows" / "email-ingestion-v1"


def dedent(code: str) -> str:
	return textwrap.dedent(code).strip("\n")


def workflow(
	workflow_id: str,
	name: str,
	description: str,
	nodes: list[dict],
	connections: dict,
	*,
	version_id: str,
	trigger_count: int,
) -> dict:
	return {
		"id": workflow_id,
		"name": name,
		"description": description,
		"active": False,
		"isArchived": False,
		"nodes": nodes,
		"connections": connections,
		"settings": {
			"executionOrder": "v1",
			"availableInMCP": False,
		},
		"pinData": {},
		"versionId": version_id,
		"activeVersionId": None,
		"versionCounter": 1,
		"triggerCount": trigger_count,
		"tags": [],
		"meta": {
			"templateCredsSetupCompleted": False,
		},
		"versionMetadata": {
			"name": None,
			"description": description,
		},
	}


TRIAGE_CODE = dedent(
	"""
	const item = $input.first().json;

	const normalized = (...values) =>
	  values
	    .filter((value) => typeof value === 'string' && value.trim().length > 0)
	    .join(' ')
	    .replace(/<[^>]+>/g, ' ')
	    .replace(/\\s+/g, ' ')
	    .trim();

	const clamp = (value, min, max) => Math.min(max, Math.max(min, value));

	const sourceAccount = (item.sourceAccount || '').trim().toLowerCase();
	const sender = (item.sender || '').trim();
	const subject = (item.subject || '').trim();
	const receivedAt = item.receivedAt || new Date().toISOString();
	const currency = (item.currency || 'BRL').trim().toUpperCase();
	const textBody = normalized(item.textBody || item.text || '', item.snippet || '', item.htmlBody || '');
	const lowered = normalized(sender, subject, textBody).toLowerCase();
	const DEFAULT_REVIEW_CATEGORY = 'Geral';
	const DEFAULT_REVIEW_SUBCATEGORY = 'Primeiros lançamentos';
	const amountPattern = /(?:R\\$\\s*|US\\$\\s*|USD\\s*|EUR\\s*|€\\s*|GBP\\s*|£\\s*)?-?\\d[\\d.,]*[\\.,]\\d{2}/gi;

	const buildRawReference = () => {
	  const base = `${sourceAccount}|${item.externalMessageId || ''}|${sender}|${subject}`;
	  return base.length > 500 ? base.slice(0, 500) : base;
	};

	const parseAmountToken = (rawValue) => {
	  if (rawValue == null) return null;
	  if (typeof rawValue === 'number') {
	    return Number.isFinite(rawValue) ? rawValue : null;
	  }
	  const sanitized = String(rawValue)
	    .trim()
	    .replace(/(?:R\\$|US\\$|USD|EUR|GBP|€|£)\\s*/gi, '')
	    .replace(/\\s+/g, '');
	  if (!sanitized) return null;
	  const negative = sanitized.startsWith('-');
	  const unsigned = sanitized.replace(/-/g, '');
	  const lastComma = unsigned.lastIndexOf(',');
	  const lastDot = unsigned.lastIndexOf('.');
	  let normalizedValue = unsigned;
	  if (lastComma >= 0 && lastDot >= 0) {
	    if (lastComma > lastDot) {
	      normalizedValue = unsigned.replace(/\\./g, '').replace(',', '.');
	    } else {
	      normalizedValue = unsigned.replace(/,/g, '');
	    }
	  } else if (lastComma >= 0) {
	    const decimals = unsigned.length - lastComma - 1;
	    normalizedValue = decimals === 2
	      ? unsigned.replace(/\\./g, '').replace(',', '.')
	      : unsigned.replace(/,/g, '');
	  } else if (lastDot >= 0) {
	    const decimals = unsigned.length - lastDot - 1;
	    normalizedValue = decimals === 2
	      ? unsigned.replace(/,/g, '')
	      : unsigned.replace(/\\./g, '');
	  }
	  if (!/^\\d+(?:\\.\\d+)?$/.test(normalizedValue)) {
	    return null;
	  }
	  const parsed = Number(normalizedValue);
	  if (!Number.isFinite(parsed)) {
	    return null;
	  }
	  return negative ? -parsed : parsed;
	};

	const extractMaxAmount = (value) => {
	  if (!value) return null;
	  const matches = value.match(amountPattern) || [];
	  const parsed = matches
	    .map((match) => parseAmountToken(match))
	    .filter((numberValue) => numberValue !== null);
	  if (!parsed.length) return null;
	  return Math.max(...parsed);
	};

	const extractDate = (value) => {
	  if (!value) return null;
	  const iso = value.match(/\\b(20\\d{2}-\\d{2}-\\d{2})\\b/);
	  if (iso) return iso[1];
	  const brazilian = value.match(/\\b(\\d{2})\\/(\\d{2})\\/(20\\d{2})\\b/);
	  if (brazilian) return `${brazilian[3]}-${brazilian[2]}-${brazilian[1]}`;
	  return null;
	};

	const parseItems = (value) => {
	  if (!value) return [];
	  const items = [];
	  for (const rawLine of value.split(/\\r?\\n/)) {
	    const line = rawLine.trim();
	    if (!line) continue;
	    const amountMatch = line.match(amountPattern);
	    if (!amountMatch) continue;
	    const amount = parseAmountToken(amountMatch[0]);
	    if (!Number.isFinite(amount)) continue;
	    const description = line
	      .replace(/^[\\-•*]+\\s*/, '')
	      .replace(amountMatch[0], '')
	      .replace(/[xX]\\s*\\d+$/g, '')
	      .replace(/[\\-–—:]$/g, '')
	      .trim();
	    if (!description) continue;
	    const normalizedDescription = description
	      .toLowerCase()
	      .normalize('NFD')
	      .replace(/[\\u0300-\\u036f]/g, '');
	    const comparableDescription = normalizedDescription
	      .replace(/[^a-z0-9\\s]/g, ' ')
	      .replace(/\\s+/g, ' ')
	      .trim();
	    if (comparableDescription === 'total' || comparableDescription === 'valor total') continue;
	    items.push({
	      description,
	      amount,
	    });
	  }
	  return items.slice(0, 15);
	};

	const resolveReviewCatalog = () => {
	  if (lowered.includes('internet') || lowered.includes('banda larga') || lowered.includes('fibra') || lowered.includes('wi-fi')) {
	    return {
	      suggestedCategoryName: 'Casa',
	      suggestedSubcategoryName: 'Internet',
	    };
	  }
	  if (lowered.includes('mercado') || lowered.includes('supermercado') || lowered.includes('mercearia')) {
	    return {
	      suggestedCategoryName: 'Casa',
	      suggestedSubcategoryName: 'Mercado',
	    };
	  }
	  return {
	    suggestedCategoryName: DEFAULT_REVIEW_CATEGORY,
	    suggestedSubcategoryName: DEFAULT_REVIEW_SUBCATEGORY,
	  };
	};

	const ignoreSignals = [
	  'unsubscribe',
	  'descadastre',
	  'promoção',
	  'promocao',
	  'oferta',
	  'cupom',
	  'newsletter',
	  'black friday',
	  'liquidação',
	  'liquidacao',
	  'instagram',
	  'linkedin',
	  'facebook',
	  'social',
	];

	const financeSignals = [
	  'fatura',
	  'boleto',
	  'vencimento',
	  'mensalidade',
	  'parcela',
	  'recibo',
	  'pagamento',
	  'compra',
	  'nota fiscal',
	  'tarifa',
	  'assinatura',
	  'conta',
	];

	const manualSignals = [
	  'compra',
	  'comprei',
	  'itens',
	  'total',
	  'pet shop',
	  'cobasi',
	  'manual@',
	  'resumo da compra',
	];

	const recurringMapping = () => {
	  if (lowered.includes('internet') || lowered.includes('banda larga') || lowered.includes('fibra') || lowered.includes('wi-fi')) {
	    return {
	      merchantOrPayee: item.merchantOrPayee || 'Internet',
	      suggestedCategoryName: 'Casa',
	      suggestedSubcategoryName: 'Internet',
	      desiredDecision: 'AUTO_IMPORT',
	      confidence: 0.97,
	    };
	  }
	  if (lowered.includes('mercado') || lowered.includes('supermercado')) {
	    return {
	      merchantOrPayee: item.merchantOrPayee || 'Mercado',
	      suggestedCategoryName: 'Casa',
	      suggestedSubcategoryName: 'Mercado',
	      desiredDecision: 'AUTO_IMPORT',
	      confidence: 0.94,
	    };
	  }
	  if (lowered.includes('energia') || lowered.includes('luz') || lowered.includes('gas') || lowered.includes('gás')
	    || lowered.includes('celular') || lowered.includes('telefone') || lowered.includes('internet móvel')
	    || lowered.includes('internet movel')) {
	    const reviewCatalog = resolveReviewCatalog();
	    return {
	      merchantOrPayee: item.merchantOrPayee || sender || subject,
	      suggestedCategoryName: reviewCatalog.suggestedCategoryName,
	      suggestedSubcategoryName: reviewCatalog.suggestedSubcategoryName,
	      desiredDecision: 'REVIEW',
	      confidence: 0.9,
	    };
	  }
	  if (lowered.includes('faculdade') || lowered.includes('universidade') || lowered.includes('mba') || lowered.includes('curso')) {
	    const reviewCatalog = resolveReviewCatalog();
	    return {
	      merchantOrPayee: item.merchantOrPayee || sender || subject,
	      suggestedCategoryName: reviewCatalog.suggestedCategoryName,
	      suggestedSubcategoryName: reviewCatalog.suggestedSubcategoryName,
	      desiredDecision: 'REVIEW',
	      confidence: 0.86,
	    };
	  }
	  if (lowered.includes('banco') || lowered.includes('tarifa') || lowered.includes('anuidade')) {
	    const reviewCatalog = resolveReviewCatalog();
	    return {
	      merchantOrPayee: item.merchantOrPayee || sender || subject,
	      suggestedCategoryName: reviewCatalog.suggestedCategoryName,
	      suggestedSubcategoryName: reviewCatalog.suggestedSubcategoryName,
	      desiredDecision: 'REVIEW',
	      confidence: 0.82,
	    };
	  }
	  return null;
	};

	const manualStructuredMapping = () => {
	  const hasEnoughStructure = parsedItems.length > 0 && effectiveTotal != null;
	  if (!hasEnoughStructure) return null;
	  if (!hasManualSignal && !subject.toLowerCase().includes('compra') && !lowered.includes('resumo da compra')) {
	    return null;
	  }

	  const petSignals = ['cobasi', 'pet shop', 'petshop', 'ração', 'racao', 'petisco', 'areia'];
	  const hasPetSignal = petSignals.some((signal) => lowered.includes(signal));
	  const defaultMerchant = item.merchantOrPayee
	    || (lowered.includes('cobasi') ? 'Cobasi' : '')
	    || sender
	    || subject;
	  const reviewCatalog = resolveReviewCatalog();

	  return {
	    merchantOrPayee: defaultMerchant,
	    suggestedCategoryName: reviewCatalog.suggestedCategoryName,
	    suggestedSubcategoryName: reviewCatalog.suggestedSubcategoryName,
	    desiredDecision: 'REVIEW',
	    confidence: hasPetSignal ? 0.88 : 0.8,
	    summary: (item.summary || subject || defaultMerchant || 'Compra manual detectada').slice(0, 500),
	  };
	};

	const incompleteManualReview = () => {
	  if (!hasManualSignal) return null;
	  if (effectiveTotal != null || parsedItems.length > 0) return null;
	  const reviewCatalog = resolveReviewCatalog();
	  const defaultMerchant = item.merchantOrPayee || sender || subject;
	  return {
	    merchantOrPayee: defaultMerchant,
	    suggestedCategoryName: reviewCatalog.suggestedCategoryName,
	    suggestedSubcategoryName: reviewCatalog.suggestedSubcategoryName,
	    desiredDecision: 'REVIEW',
	    confidence: 0.55,
	    summary: (item.summary || subject || defaultMerchant || 'Compra manual com dados insuficientes').slice(0, 500),
	  };
	};

	const amount = parseAmountToken(item.totalAmount) ?? extractMaxAmount(lowered);
	const parsedItems = Array.isArray(item.items) && item.items.length ? item.items : parseItems(item.textBody || item.text || '');
	const effectiveTotal = amount ?? (parsedItems.length ? parsedItems.reduce((sum, current) => sum + (current.amount || 0), 0) : null);
	const dueDate = item.dueDate || extractDate(lowered);
	const occurredOn = item.occurredOn || extractDate(lowered);
	const hasIgnoreSignal = ignoreSignals.some((signal) => lowered.includes(signal));
	const hasFinanceSignal = financeSignals.some((signal) => lowered.includes(signal));
	const hasManualSignal = manualSignals.some((signal) => lowered.includes(signal));
	const knownMapping = recurringMapping();
	const manualStructured = manualStructuredMapping();
	const incompleteManual = incompleteManualReview();

	if (hasIgnoreSignal && !hasFinanceSignal) {
	  return [{
	    json: {
	      ...item,
	      route: 'ignore',
	      aiUsed: false,
	      classification: 'IRRELEVANT',
	      desiredDecision: 'IGNORE',
	      confidence: 0.05,
	      merchantOrPayee: item.merchantOrPayee || sender,
	      suggestedCategoryName: null,
	      suggestedSubcategoryName: null,
	      totalAmount: null,
	      dueDate: null,
	      occurredOn: null,
	      items: [],
	      summary: `Ignorado por heurística barata: ${subject || 'sem assunto'}`.slice(0, 500),
	      rawReference: buildRawReference(),
	      extractionText: null,
	    },
	  }];
	}

	if (knownMapping) {
	  return [{
	    json: {
	      ...item,
	      route: 'direct',
	      aiUsed: false,
	      classification: 'RECURRING_BILL',
	      desiredDecision: knownMapping.desiredDecision,
	      confidence: knownMapping.confidence,
	      merchantOrPayee: knownMapping.merchantOrPayee,
	      suggestedCategoryName: knownMapping.suggestedCategoryName,
	      suggestedSubcategoryName: knownMapping.suggestedSubcategoryName,
	      totalAmount: effectiveTotal,
	      dueDate,
	      occurredOn,
	      items: parsedItems,
	      summary: (item.summary || subject || sender || 'Cobrança recorrente detectada').slice(0, 500),
	      rawReference: buildRawReference(),
	      extractionText: null,
	    },
	  }];
	}

	if (manualStructured) {
	  return [{
	    json: {
	      ...item,
	      route: 'direct',
	      aiUsed: false,
	      classification: 'MANUAL_PURCHASE',
	      desiredDecision: manualStructured.desiredDecision,
	      confidence: manualStructured.confidence,
	      merchantOrPayee: manualStructured.merchantOrPayee,
	      suggestedCategoryName: manualStructured.suggestedCategoryName,
	      suggestedSubcategoryName: manualStructured.suggestedSubcategoryName,
	      totalAmount: effectiveTotal,
	      dueDate,
	      occurredOn,
	      items: parsedItems,
	      summary: manualStructured.summary,
	      rawReference: buildRawReference(),
	      extractionText: null,
	    },
	  }];
	}

	if (incompleteManual) {
	  return [{
	    json: {
	      ...item,
	      route: 'direct',
	      aiUsed: false,
	      classification: 'MANUAL_PURCHASE',
	      desiredDecision: incompleteManual.desiredDecision,
	      confidence: incompleteManual.confidence,
	      merchantOrPayee: incompleteManual.merchantOrPayee,
	      suggestedCategoryName: incompleteManual.suggestedCategoryName,
	      suggestedSubcategoryName: incompleteManual.suggestedSubcategoryName,
	      totalAmount: null,
	      dueDate,
	      occurredOn,
	      items: [],
	      summary: incompleteManual.summary,
	      rawReference: buildRawReference(),
	      extractionText: null,
	    },
	  }];
	}

	if (hasManualSignal || (hasFinanceSignal && (textBody.length > 60 || parsedItems.length > 0))) {
	  return [{
	    json: {
	      ...item,
	      route: 'ai',
	      aiUsed: true,
	      parsedItems,
	      totalAmount: effectiveTotal,
	      dueDate,
	      occurredOn,
	      rawReference: buildRawReference(),
	      extractionText: normalized(
	        `sender: ${sender}`,
	        `subject: ${subject}`,
	        `body: ${textBody.slice(0, 4000)}`,
	      ),
	    },
	  }];
	}

	return [{
	  json: {
	    ...item,
	    route: 'ignore',
	    aiUsed: false,
	    classification: 'IRRELEVANT',
	    desiredDecision: 'IGNORE',
	    confidence: 0.1,
	    merchantOrPayee: item.merchantOrPayee || sender,
	    suggestedCategoryName: null,
	    suggestedSubcategoryName: null,
	    totalAmount: null,
	    dueDate: null,
	    occurredOn: null,
	    items: [],
	    summary: 'Ignorado por ausência de sinais financeiros suficientes',
	    rawReference: buildRawReference(),
	    extractionText: null,
	  },
	}];
	"""
)

NORMALIZE_ENVELOPE_CODE = dedent(
	"""
	const envelope = $input.first().json;
	const payload = envelope && typeof envelope.body === 'object' && envelope.body !== null
	  ? envelope.body
	  : envelope;

	return [{
	  json: {
	    ...payload,
	    backendBaseUrl: payload.backendBaseUrl || envelope.backendBaseUrl || $vars.DESPESAS_BACKEND_BASE_URL || '',
	    operationalToken: payload.operationalToken || envelope.operationalToken || $vars.DESPESAS_OPERATIONAL_EMAIL_INGESTION_TOKEN || '',
	  },
	}];
	"""
)

BUILD_DETERMINISTIC_PAYLOAD_CODE = dedent(
	"""
	const item = $input.first().json;

	const payload = {
	  sourceAccount: item.sourceAccount,
	  externalMessageId: item.externalMessageId,
	  sender: item.sender,
	  subject: item.subject,
	  receivedAt: item.receivedAt,
	  merchantOrPayee: item.merchantOrPayee || null,
	  suggestedCategoryName: item.suggestedCategoryName || null,
	  suggestedSubcategoryName: item.suggestedSubcategoryName || null,
	  totalAmount: item.totalAmount ?? null,
	  dueDate: item.dueDate || null,
	  occurredOn: item.occurredOn || null,
	  currency: item.currency || 'BRL',
	  items: Array.isArray(item.items) ? item.items : [],
	  summary: item.summary || null,
	  classification: item.classification,
	  confidence: item.confidence,
	  rawReference: item.rawReference,
	  desiredDecision: item.desiredDecision,
	};

	return [{
	  json: {
	    ...item,
	    backendPayload: payload,
	  },
	}];
	"""
)

BUILD_AI_PAYLOAD_CODE = dedent(
	"""
	const item = $input.first().json;
	const extracted = item.output || {};

	const safeString = (value, fallback = null) => {
	  if (typeof value !== 'string') return fallback;
	  const trimmed = value.trim();
	  return trimmed ? trimmed : fallback;
	};

	const clamp = (value, min, max) => Math.min(max, Math.max(min, value));
	const normalizedClassification = safeString(extracted.classification, 'MANUAL_PURCHASE');
	const normalizedCategory = safeString(extracted.suggestedCategoryName);
	const normalizedSubcategory = safeString(extracted.suggestedSubcategoryName);
	const confidence = clamp(Number(extracted.confidence ?? item.confidence ?? 0.7), 0, 1);
	const defaultSummary = safeString(extracted.summary, safeString(item.subject, 'Despesa extraída por IA'));
	const defaultMerchant = safeString(extracted.merchantOrPayee, safeString(item.sender, 'Origem não identificada'));
	const safeTotal = extracted.totalAmount == null ? (item.totalAmount == null ? null : Number(item.totalAmount)) : Number(extracted.totalAmount);

	let desiredDecision = 'REVIEW';
	if (normalizedClassification === 'IRRELEVANT') {
	  desiredDecision = 'IGNORE';
	} else if (
	  confidence >= 0.94 &&
	  normalizedCategory === 'Casa' &&
	  (normalizedSubcategory === 'Internet' || normalizedSubcategory === 'Mercado')
	) {
	  desiredDecision = 'AUTO_IMPORT';
	}

	const payload = {
	  sourceAccount: item.sourceAccount,
	  externalMessageId: item.externalMessageId,
	  sender: item.sender,
	  subject: item.subject,
	  receivedAt: item.receivedAt,
	  merchantOrPayee: defaultMerchant,
	  suggestedCategoryName: normalizedCategory,
	  suggestedSubcategoryName: normalizedSubcategory,
	  totalAmount: safeTotal,
	  dueDate: safeString(extracted.dueDate, item.dueDate || null),
	  occurredOn: safeString(extracted.occurredOn, item.occurredOn || null),
	  currency: item.currency || 'BRL',
	  items: Array.isArray(item.parsedItems) ? item.parsedItems : [],
	  summary: defaultSummary,
	  classification: normalizedClassification,
	  confidence,
	  rawReference: item.rawReference,
	  desiredDecision,
	};

	return [{
	  json: {
	    ...item,
	    aiUsed: true,
	    backendPayload: payload,
	  },
	}];
	"""
)

POST_TO_BACKEND_CODE = dedent(
	"""
	const item = $input.first().json;

	if (!item.backendBaseUrl || !item.operationalToken) {
	  return [{
	    json: {
	      status: 'configuration_error',
	      aiUsed: !!item.aiUsed,
	      route: item.route,
	      mailboxAction: 'review',
	      candidate: item.backendPayload || null,
	      message: 'Backend runtime configuration is missing in n8n. Provide backendBaseUrl and operationalToken or configure n8n variables.',
	    },
	  }];
	}

	try {
	  const response = await helpers.httpRequest({
	    method: 'POST',
	    url: `${item.backendBaseUrl.replace(/\\/$/, '')}/api/v1/operations/email-ingestions`,
	    headers: {
	      'Content-Type': 'application/json',
	      'Authorization': `Bearer ${item.operationalToken}`,
	    },
	    body: item.backendPayload,
	    returnFullResponse: true,
	  });

	  const result = response.body?.data ?? response.body;
	  let mailboxAction = 'review';
	  if (result?.duplicate) {
	    mailboxAction = 'duplicate';
	  } else if (result?.decision === 'AUTO_IMPORTED') {
	    mailboxAction = 'imported';
	  } else if (result?.decision === 'IGNORED') {
	    mailboxAction = 'ignored';
	  }

	  return [{
	    json: {
	      status: 'processed',
	      aiUsed: !!item.aiUsed,
	      route: item.route,
	      mailboxAction,
	      candidate: item.backendPayload,
	      backend: result,
	      mailbox: item.mailbox || null,
	    },
	  }];
	} catch (error) {
	  const backendStatus = error?.response?.status ?? error?.statusCode ?? null;
	  const backendBody = error?.response?.data ?? error?.response?.body ?? null;
	  if (backendStatus) {
	    return [{
	      json: {
	        status: 'backend_error',
	        aiUsed: !!item.aiUsed,
	        route: item.route,
	        mailboxAction: 'review',
	        candidate: item.backendPayload,
	        backendStatus,
	        message: 'Backend rejected the ingestion candidate',
	        backendBody,
	        mailbox: item.mailbox || null,
	      },
	    }];
	  }
	  return [{
	    json: {
	      status: 'backend_unreachable',
	      aiUsed: !!item.aiUsed,
	      route: item.route,
	      mailboxAction: 'review',
	      candidate: item.backendPayload || null,
	      message: `Failed to reach backend: ${String(error.message || error).slice(0, 180)}`,
	      mailbox: item.mailbox || null,
	    },
	  }];
	}
	"""
)

REPLAY_SAMPLE_CODE = dedent(
	"""
	const envelope = $input.first().json;
	const incoming = envelope && typeof envelope.body === 'object' && envelope.body !== null
	  ? envelope.body
	  : envelope;
	const scenario = incoming.scenario || 'recurringBill';

	const samples = {
	  recurringBill: {
	    sourceAccount: incoming.sourceAccount || 'financeiro@gmail.com',
	    externalMessageId: incoming.externalMessageId || `replay-recurring-${Date.now()}`,
	    sender: incoming.sender || 'fatura@provedor.com.br',
	    subject: incoming.subject || 'Fatura Internet março',
	    receivedAt: incoming.receivedAt || new Date().toISOString(),
	    textBody: incoming.textBody || 'Olá, sua cobrança de internet residencial venceu em 25/03/2026. Valor total: R$ 129,90. Obrigado.',
	    merchantOrPayee: incoming.merchantOrPayee || 'Provedor Internet',
	    currency: incoming.currency || 'BRL',
	  },
	  manualPurchase: {
	    sourceAccount: incoming.sourceAccount || 'financeiro@gmail.com',
	    externalMessageId: incoming.externalMessageId || `replay-manual-${Date.now()}`,
	    sender: incoming.sender || 'manual@local.invalid',
	    subject: incoming.subject || 'Compra Cobasi',
	    receivedAt: incoming.receivedAt || new Date().toISOString(),
	    textBody: incoming.textBody || 'Compra no pet shop Cobasi\\nRação R$ 199,90\\nAreia R$ 49,90\\nPetisco R$ 39,90\\nTotal R$ 289,70',
	    merchantOrPayee: incoming.merchantOrPayee || 'Cobasi',
	    currency: incoming.currency || 'BRL',
	  },
	  irrelevantNewsletter: {
	    sourceAccount: incoming.sourceAccount || 'financeiro@gmail.com',
	    externalMessageId: incoming.externalMessageId || `replay-ignore-${Date.now()}`,
	    sender: incoming.sender || 'newsletter@loja.com',
	    subject: incoming.subject || 'Cupom imperdível para você',
	    receivedAt: incoming.receivedAt || new Date().toISOString(),
	    textBody: incoming.textBody || 'Oferta especial, descontos e novidades da semana. unsubscribe',
	    merchantOrPayee: incoming.merchantOrPayee || 'Loja',
	    currency: incoming.currency || 'BRL',
	  },
	};

const base = samples[scenario] || samples.recurringBill;
const processorBaseUrl = incoming.processorBaseUrl
  || $vars.WEBHOOK_URL
  || `${$vars.N8N_PROTOCOL || 'http'}://${$vars.N8N_HOST || 'localhost'}:${$vars.N8N_PORT || '5678'}`;
return [{
  json: {
    processorBaseUrl,
    processorPayload: {
      ...base,
      channel: incoming.channel || 'replay',
      backendBaseUrl: incoming.backendBaseUrl || '',
      operationalToken: incoming.operationalToken || '',
    },
  },
}];
	"""
)

FORWARD_TO_PROCESSOR_CODE = dedent(
	"""
	const outputs = [];

	for (const entry of $input.all()) {
	  const item = entry.json;
	  const processorBaseUrl = item.processorBaseUrl
	    || $vars.WEBHOOK_URL
	    || `${$vars.N8N_PROTOCOL || 'http'}://${$vars.N8N_HOST || 'localhost'}:${$vars.N8N_PORT || '5678'}`;
	  const processorUrl = `${processorBaseUrl.replace(/\\/$/, '')}/webhook/email-ingestion-process-v1`;

	  try {
	    const response = await helpers.httpRequest({
	      method: 'POST',
	      url: processorUrl,
	      headers: {
	        'Content-Type': 'application/json',
	      },
	      body: item.processorPayload,
	      returnFullResponse: true,
	    });
	    const responseBody = response.body;
	    const safeJson = responseBody && typeof responseBody === 'object'
	      ? responseBody
	      : null;
	    outputs.push({
	      json: safeJson
	        ? {
	            ...safeJson,
	            mailbox: safeJson.mailbox || item.processorPayload?.mailbox || null,
	          }
	        : {
	            status: 'processor_unparseable_response',
	            httpStatus: response.statusCode,
	            body: responseBody ?? null,
	            mailbox: item.processorPayload?.mailbox || null,
	          },
	    });
	  } catch (error) {
	    const httpStatus = error?.response?.status ?? error?.statusCode ?? null;
	    const body = error?.response?.data ?? error?.response?.body ?? null;
	    if (httpStatus) {
	      outputs.push({
	        json: {
	          status: 'processor_error',
	          httpStatus,
	          body,
	          mailbox: item.processorPayload?.mailbox || null,
	        },
	      });
	      continue;
	    }
	    outputs.push({
	      json: {
	        status: 'processor_unreachable',
	        message: `Failed to reach processor workflow: ${String(error.message || error).slice(0, 180)}`,
	        mailbox: item.processorPayload?.mailbox || null,
	      },
	    });
	  }
	}

	return outputs;
	"""
)

GMAIL_PREPARE_CODE = dedent(
	"""
	return $input.all().map((entry) => {
	  const item = entry.json;
	  const headers = Array.isArray(item.headers) ? item.headers : [];
	  const findHeader = (name) => headers.find((header) => (header.name || '').toLowerCase() === name.toLowerCase())?.value || '';
	  const normalizeSender = (fromValue) => {
	    if (!fromValue) return '';
	    if (typeof fromValue === 'string') return fromValue;
	    if (typeof fromValue?.text === 'string' && fromValue.text.trim()) return fromValue.text.trim();
	    const firstAddress = Array.isArray(fromValue?.value) ? fromValue.value.find((entry) => typeof entry?.address === 'string' && entry.address.trim()) : null;
	    if (firstAddress?.address) return firstAddress.address.trim();
	    return '';
	  };

	  return {
	    json: {
	      processorBaseUrl: $vars.WEBHOOK_URL || `${$vars.N8N_PROTOCOL || 'http'}://${$vars.N8N_HOST || 'localhost'}:${$vars.N8N_PORT || '5678'}`,
	      processorPayload: {
	        channel: 'gmail',
	        sourceAccount: $vars.DESPESAS_GMAIL_SOURCE_ACCOUNT || findHeader('Delivered-To') || findHeader('To') || '',
	        externalMessageId: item.messageId || item.id || '',
	        sender: normalizeSender(item.from) || findHeader('From') || '',
	        subject: item.subject || findHeader('Subject') || '',
	        receivedAt: item.date || item.internalDate || new Date().toISOString(),
	        textBody: item.text || item.snippet || '',
	        htmlBody: item.html || '',
	        merchantOrPayee: '',
	        currency: 'BRL',
	        backendBaseUrl: $vars.DESPESAS_BACKEND_BASE_URL || '',
	        operationalToken: $vars.DESPESAS_OPERATIONAL_EMAIL_INGESTION_TOKEN || '',
	        mailbox: {
	          provider: 'gmail',
	          messageId: item.id || item.messageId || '',
	          threadId: item.threadId || '',
	        },
	      },
	    },
	  };
	});
	"""
)

OUTLOOK_PREPARE_CODE = dedent(
	"""
	return $input.all().map((entry) => {
	  const item = entry.json;
	  const fromAddress = item.from && item.from.emailAddress ? item.from.emailAddress.address : (item.from || '');

	  return {
	    json: {
	      processorBaseUrl: $vars.WEBHOOK_URL || `${$vars.N8N_PROTOCOL || 'http'}://${$vars.N8N_HOST || 'localhost'}:${$vars.N8N_PORT || '5678'}`,
	      processorPayload: {
	        channel: 'outlook',
	        sourceAccount: $vars.DESPESAS_OUTLOOK_SOURCE_ACCOUNT || '',
	        externalMessageId: item.id || item.internetMessageId || '',
	        sender: fromAddress,
	        subject: item.subject || '',
	        receivedAt: item.receivedDateTime || new Date().toISOString(),
	        textBody: item.bodyPreview || (item.body ? item.body.content : ''),
	        htmlBody: item.body ? item.body.content : '',
	        merchantOrPayee: '',
	        currency: 'BRL',
	        backendBaseUrl: $vars.DESPESAS_BACKEND_BASE_URL || '',
	        operationalToken: $vars.DESPESAS_OPERATIONAL_EMAIL_INGESTION_TOKEN || '',
	        mailbox: {
	          provider: 'outlook',
	          messageId: item.id || '',
	        },
	      },
	    },
	  };
	});
	"""
)

MAILBOX_ACTION_CODE = dedent(
	"""
	return $input.all().map((entry) => {
	  const item = entry.json;
	  const backend = item.backend || {};
	  return {
	    json: {
	      ...item,
	      mailboxAction: item.mailboxAction || (backend.duplicate ? 'duplicate' : backend.decision === 'AUTO_IMPORTED' ? 'imported' : backend.decision === 'IGNORED' ? 'ignored' : 'review'),
	    },
	  };
	});
	"""
)

GMAIL_MAILBOX_PLAN_CODE = dedent(
	"""
	const labelByAction = {
	  imported: 'Despesas/Imported',
	  review: 'Despesas/Review',
	  ignored: 'Despesas/Ignored',
	  duplicate: 'Despesas/Duplicate',
	};

	return $input.all().map((entry) => {
	  const item = entry.json;
	  const mailbox = item.mailbox || {};
	  const shouldLabel = item.status === 'processed' && typeof mailbox.messageId === 'string' && mailbox.messageId.trim().length > 0;
	  const gmailLabelName = shouldLabel
	    ? (labelByAction[item.mailboxAction] || labelByAction.review)
	    : null;

	  return {
	    json: {
	      ...item,
	      mailboxUpdateMode: shouldLabel ? 'label' : 'none',
	      gmailLabelName,
	    },
	  };
	});
	"""
)

OUTLOOK_MAILBOX_PLAN_CODE = dedent(
	"""
	const folderByAction = {
	  imported: 'Despesas Imported',
	  review: 'Despesas Review',
	  ignored: 'Despesas Ignored',
	  duplicate: 'Despesas Duplicate',
	};

	return $input.all().map((entry) => {
	  const item = entry.json;
	  const mailbox = item.mailbox || {};
	  const shouldMove = item.status === 'processed' && typeof mailbox.messageId === 'string' && mailbox.messageId.trim().length > 0;
	  const outlookTargetFolderName = shouldMove
	    ? (folderByAction[item.mailboxAction] || folderByAction.review)
	    : null;
	  const escapedFolderName = outlookTargetFolderName ? outlookTargetFolderName.replace(/'/g, "''") : '';

	  return {
	    json: {
	      ...item,
	      mailboxUpdateMode: shouldMove ? 'move' : 'none',
	      outlookTargetFolderName,
	      outlookFolderFilter: outlookTargetFolderName ? `displayName eq '${escapedFolderName}'` : '',
	    },
	  };
	});
	"""
)

GMAIL_RESOLVE_LABEL_ID_CODE = dedent(
	"""
	const plannedItems = $('Plan Gmail Mailbox Action').all();
	const labels = $input
	  .all()
	  .map((entry) => entry.json || {})
	  .filter((label) => typeof label.name === 'string' && label.name.trim().length > 0);

	return plannedItems.map((entry) => {
	  const item = entry.json;
	  const matchedLabel = labels.find((label) => (label.name || '').trim() === item.gmailLabelName);

	  return {
	    json: {
	      ...item,
	      gmailLabelId: matchedLabel?.id || null,
	    },
	  };
	});
	"""
)

FILTER_GMAIL_LABEL_TARGETS_CODE = dedent(
	"""
	return $input.all().filter((entry) => {
	  const item = entry.json;
	  return item.mailboxUpdateMode === 'label'
	    && typeof item.gmailLabelId === 'string'
	    && item.gmailLabelId.trim().length > 0
	    && typeof item.mailbox?.messageId === 'string'
	    && item.mailbox.messageId.trim().length > 0;
	});
	"""
)

OUTLOOK_RESOLVE_FOLDER_ID_CODE = dedent(
	"""
	const plannedItems = $('Plan Outlook Mailbox Action').all();
	const folders = $input
	  .all()
	  .map((entry) => entry.json || {})
	  .filter((folder) => typeof folder.displayName === 'string' && folder.displayName.trim().length > 0);

	return plannedItems.map((entry) => {
	  const item = entry.json;
	  const matchedFolder = folders.find((folder) => (folder.displayName || '').trim() === item.outlookTargetFolderName);

	  return {
	    json: {
	      ...item,
	      outlookTargetFolderId: matchedFolder?.id || null,
	    },
	  };
	});
	"""
)

FILTER_OUTLOOK_MOVE_TARGETS_CODE = dedent(
	"""
	return $input.all().filter((entry) => {
	  const item = entry.json;
	  return item.mailboxUpdateMode === 'move'
	    && typeof item.outlookTargetFolderId === 'string'
	    && item.outlookTargetFolderId.trim().length > 0
	    && typeof item.mailbox?.messageId === 'string'
	    && item.mailbox.messageId.trim().length > 0;
	});
	"""
)

GMAIL_LABEL_BOOTSTRAP_CODE = dedent(
	"""
	const expectedLabels = [
	  'Despesas/Imported',
	  'Despesas/Review',
	  'Despesas/Ignored',
	  'Despesas/Duplicate',
	];

	const existing = new Set(
	  $input.all()
	    .map((entry) => typeof entry.json?.name === 'string' ? entry.json.name.trim() : '')
	    .filter(Boolean)
	);

	return expectedLabels
	  .filter((labelName) => !existing.has(labelName))
	  .map((labelName) => ({
	    json: {
	      labelName,
	    },
	  }));
	"""
)

OUTLOOK_FOLDER_BOOTSTRAP_CODE = dedent(
	"""
	const expectedFolders = [
	  'Despesas Imported',
	  'Despesas Review',
	  'Despesas Ignored',
	  'Despesas Duplicate',
	];

	const existing = new Set(
	  $input.all()
	    .map((entry) => typeof entry.json?.displayName === 'string' ? entry.json.displayName.trim() : '')
	    .filter(Boolean)
	);

	return expectedFolders
	  .filter((folderName) => !existing.has(folderName))
	  .map((folderName) => ({
	    json: {
	      folderName,
	    },
	  }));
	"""
)

LIVE_SENDER_CODE = dedent(
	"""
	const envelope = $input.first().json;
	const incoming = envelope && typeof envelope.body === 'object' && envelope.body !== null
	  ? envelope.body
	  : envelope;

	const batchTag = typeof incoming.batchTag === 'string' && incoming.batchTag.trim().length > 0
	  ? incoming.batchTag.trim()
	  : new Date().toISOString().replace(/\\D/g, '').slice(0, 14);

	const gmailTo = incoming.gmailTo || $vars.DESPESAS_SMOKE_GMAIL_TO || '';
	const outlookTo = incoming.outlookTo || $vars.DESPESAS_SMOKE_OUTLOOK_TO || '';
	const includeDuplicate = incoming.includeDuplicate !== false;

	if (!gmailTo || !outlookTo) {
	  throw new Error('Configure gmailTo/outlookTo no payload ou as variaveis DESPESAS_SMOKE_GMAIL_TO e DESPESAS_SMOKE_OUTLOOK_TO.');
	}

	const recurringSubject = `N8N LIVE BILL ${batchTag}`;
	const recurringBody = 'Conta internet residencial\\nVencimento 25/03/2026\\nValor total: R$ 129,90\\nInternet fibra casa';

	const messages = [
	  {
	    to: gmailTo,
	    subject: `N8N LIVE IGNORE ${batchTag}`,
	    body: 'Promocao imperdivel da semana\\nunsubscribe\\nCupom de desconto',
	  },
	  {
	    to: gmailTo,
	    subject: `N8N LIVE COBASI ${batchTag}`,
	    body: 'Compra no pet shop Cobasi\\nRacao R$ 199,90\\nAreia R$ 49,90\\nPetisco R$ 39,90\\nTotal R$ 289,70',
	  },
	  {
	    to: outlookTo,
	    subject: recurringSubject,
	    body: recurringBody,
	  },
	];

	if (includeDuplicate) {
	  messages.push({
	    to: outlookTo,
	    subject: recurringSubject,
	    body: recurringBody,
	  });
	}

	return messages.map((message) => ({ json: message }));
	"""
)


def webhook_node(node_id: str, name: str, path: str, position: list[int]) -> dict:
	return {
		"parameters": {
			"httpMethod": "POST",
			"path": path,
			"responseMode": "responseNode",
			"options": {},
		},
		"type": "n8n-nodes-base.webhook",
		"typeVersion": 2.1,
		"position": position,
		"id": node_id,
		"name": name,
		"webhookId": node_id,
	}


def webhook_on_received_node(node_id: str, name: str, path: str, position: list[int]) -> dict:
	return {
		"parameters": {
			"httpMethod": "POST",
			"path": path,
			"responseMode": "onReceived",
			"options": {},
		},
		"type": "n8n-nodes-base.webhook",
		"typeVersion": 2.1,
		"position": position,
		"id": node_id,
		"name": name,
		"webhookId": node_id,
	}


def webhook_last_node(node_id: str, name: str, path: str, position: list[int]) -> dict:
	return {
		"parameters": {
			"httpMethod": "POST",
			"path": path,
			"responseMode": "lastNode",
			"options": {},
		},
		"type": "n8n-nodes-base.webhook",
		"typeVersion": 2,
		"position": position,
		"id": node_id,
		"name": name,
		"webhookId": path,
	}


def manual_trigger_node(node_id: str, name: str, position: list[int]) -> dict:
	return {
		"parameters": {},
		"type": "n8n-nodes-base.manualTrigger",
		"typeVersion": 1,
		"position": position,
		"id": node_id,
		"name": name,
	}


def code_node(node_id: str, name: str, position: list[int], code: str) -> dict:
	return {
		"parameters": {
			"jsCode": code,
		},
		"type": "n8n-nodes-base.code",
		"typeVersion": 2,
		"position": position,
		"id": node_id,
		"name": name,
	}


def if_node(node_id: str, name: str, position: list[int], left_value: str, right_value: str) -> dict:
	return {
		"parameters": {
			"conditions": {
				"options": {
					"caseSensitive": True,
					"typeValidation": "strict",
					"version": 2,
				},
				"conditions": [
					{
						"id": f"{node_id}-condition-0",
						"leftValue": left_value,
						"rightValue": right_value,
						"operator": {
							"type": "string",
							"operation": "equals",
						},
					},
				],
				"combinator": "and",
			},
			"options": {
				"ignoreCase": False,
			},
		},
		"type": "n8n-nodes-base.if",
		"typeVersion": 2.2,
		"position": position,
		"id": node_id,
		"name": name,
	}


def set_node(node_id: str, name: str, position: list[int], assignments: list[tuple[str, str, str]]) -> dict:
	return {
		"parameters": {
			"assignments": {
				"assignments": [
					{
						"id": f"{node_id}-{index}",
						"name": field_name,
						"value": value,
						"type": field_type,
					}
					for index, (field_name, value, field_type) in enumerate(assignments)
				],
			},
			"options": {},
		},
		"type": "n8n-nodes-base.set",
		"typeVersion": 3.4,
		"position": position,
		"id": node_id,
		"name": name,
	}


def respond_node(node_id: str, name: str, position: list[int]) -> dict:
	return {
		"parameters": {
			"respondWith": "firstIncomingItem",
		},
		"type": "n8n-nodes-base.respondToWebhook",
		"typeVersion": 1.5,
		"position": position,
		"id": node_id,
		"name": name,
	}


def merge_by_position_node(node_id: str, name: str, position: list[int]) -> dict:
	return {
		"parameters": {
			"mode": "combine",
			"combineBy": "combineByPosition",
			"options": {},
		},
		"type": "n8n-nodes-base.merge",
		"typeVersion": 3.2,
		"position": position,
		"id": node_id,
		"name": name,
	}


def information_extractor_node(node_id: str, position: list[int]) -> dict:
	return {
		"parameters": {
			"text": "={{ $json.extractionText }}",
			"schemaType": "fromAttributes",
			"attributes": {
				"attributes": [
					{
						"name": "classification",
						"type": "string",
						"description": "Use only one of RECURRING_BILL, MANUAL_PURCHASE, FINANCIAL_TRANSACTION, IRRELEVANT",
						"required": True,
					},
					{
						"name": "merchantOrPayee",
						"type": "string",
						"description": "Best merchant, provider or payee name found in the email",
						"required": True,
					},
					{
						"name": "suggestedCategoryName",
						"type": "string",
						"description": "Portuguese spending category name suggestion, concise and capitalized",
						"required": False,
					},
					{
						"name": "suggestedSubcategoryName",
						"type": "string",
						"description": "Portuguese spending subcategory suggestion when obvious",
						"required": False,
					},
					{
						"name": "totalAmount",
						"type": "number",
						"description": "Total amount in BRL using dot decimal separator",
						"required": False,
					},
					{
						"name": "summary",
						"type": "string",
						"description": "Short summary of the financial event in Portuguese",
						"required": True,
					},
					{
						"name": "confidence",
						"type": "number",
						"description": "Confidence between 0 and 1. Prefer lower values when the email is incomplete or ambiguous.",
						"required": True,
					},
					{
						"name": "dueDate",
						"type": "date",
						"description": "Due date when clearly present",
						"required": False,
					},
					{
						"name": "occurredOn",
						"type": "date",
						"description": "Purchase or billing date when clearly present",
						"required": False,
					},
				],
			},
			"options": {
				"systemPromptTemplate": "You classify and extract only financial email candidates. Be concise, structured, conservative on confidence, and never invent values. Prefer IRRELEVANT when the text is marketing or social noise. Prefer REVIEW-grade confidence when category or amount is uncertain.",
			},
		},
		"type": "@n8n/n8n-nodes-langchain.informationExtractor",
		"typeVersion": 1.2,
		"position": position,
		"id": node_id,
		"name": "Information Extractor",
	}


def gemini_model_node(node_id: str, position: list[int]) -> dict:
	return {
		"parameters": {
			"modelName": "={{ $vars.GOOGLE_GEMINI_MODEL || 'models/gemini-3.1-flash-lite-preview' }}",
			"options": {
				"temperature": 0.1,
			},
		},
		"type": "@n8n/n8n-nodes-langchain.lmChatGoogleGemini",
		"typeVersion": 1,
		"position": position,
		"id": node_id,
		"name": "Google Gemini Chat Model",
	}


def gmail_trigger_node(node_id: str, position: list[int]) -> dict:
	return {
		"parameters": {
			"pollTimes": {
				"item": [
					{
						"mode": "everyMinute",
					},
				],
			},
			"authentication": "oAuth2",
			"event": "messageReceived",
			"simple": False,
			"filters": {
				"q": "newer_than:2d -category:promotions -category:social",
				"readStatus": "unread",
			},
			"options": {
				"downloadAttachments": False,
			},
		},
		"type": "n8n-nodes-base.gmailTrigger",
		"typeVersion": 1.3,
		"position": position,
		"id": node_id,
		"name": "Gmail Trigger",
	}


def gmail_action_node(node_id: str, name: str, position: list[int], parameters: dict) -> dict:
	return {
		"parameters": {
			"authentication": "oAuth2",
			**parameters,
		},
		"type": "n8n-nodes-base.gmail",
		"typeVersion": 2.2,
		"position": position,
		"id": node_id,
		"name": name,
	}


def outlook_trigger_node(node_id: str, position: list[int]) -> dict:
	return {
		"parameters": {
			"pollTimes": {
				"item": [
					{
						"mode": "everyMinute",
					},
				],
			},
			"event": "messageReceived",
			"output": "simple",
			"filters": {},
			"options": {},
		},
		"type": "n8n-nodes-base.microsoftOutlookTrigger",
		"typeVersion": 1,
		"position": position,
		"id": node_id,
		"name": "Microsoft Outlook Trigger",
	}


def outlook_action_node(node_id: str, name: str, position: list[int], parameters: dict) -> dict:
	return {
		"parameters": parameters,
		"type": "n8n-nodes-base.microsoftOutlook",
		"typeVersion": 2,
		"position": position,
		"id": node_id,
		"name": name,
	}


def outlook_send_message_node(node_id: str, name: str, position: list[int]) -> dict:
	return {
		"parameters": {
			"resource": "message",
			"operation": "send",
			"subject": "={{$json.subject}}",
			"bodyContent": "={{$json.body}}",
			"toRecipients": "={{$json.to}}",
			"additionalFields": {
				"bodyContentType": "Text",
				"saveToSentItems": True,
			},
		},
		"type": "n8n-nodes-base.microsoftOutlook",
		"typeVersion": 1,
		"position": position,
		"id": node_id,
		"name": name,
	}


def processor_workflow() -> dict:
	nodes = [
		webhook_node("8d42a55a-a5eb-4c46-8da8-a805a1f83281", "Processor Webhook", "email-ingestion-process-v1", [-1040, 160]),
		code_node("7227ee50-3d20-4c0d-8d76-620f63f3d4d8", "Normalize Envelope", [-800, 160], NORMALIZE_ENVELOPE_CODE),
		code_node("0f6929e5-cf05-4cf5-96ea-44567825f7af", "Normalize and Triage", [-560, 160], TRIAGE_CODE),
		if_node("757b26e2-8ba4-4e77-a145-673610af3fe2", "Route AI", [-300, 160], "={{ $json.route }}", "ai"),
		code_node("a73d708c-9ba4-414e-81eb-a3387bc3ae84", "Build Deterministic Payload", [-60, 20], BUILD_DETERMINISTIC_PAYLOAD_CODE),
		code_node("ef221667-498f-4600-a86c-3d104a42eb6e", "Post Deterministic Candidate", [180, 20], POST_TO_BACKEND_CODE),
		respond_node("073eaabc-5460-43e0-964f-8c8dce58f5b3", "Respond Deterministic", [420, 20]),
		information_extractor_node("0d2f16a4-4d54-4b91-996d-b950dbbc126a", [-40, 420]),
		gemini_model_node("76710016-c653-467a-ac18-c85e14dd2dd6", [-40, 700]),
		merge_by_position_node("16e24cfe-70dc-4da9-9af1-6106c871e95d", "Merge AI Output", [200, 420]),
		code_node("5a531557-813e-42bb-8f77-f22fa65bc17f", "Build AI Payload", [440, 420], BUILD_AI_PAYLOAD_CODE),
		code_node("73c15b69-c46c-40d5-9d10-11e35121a29b", "Post AI Candidate", [680, 420], POST_TO_BACKEND_CODE),
		respond_node("1bcd0ec8-1770-46a7-ad33-77bef73e8b8f", "Respond AI", [920, 420]),
	]

	connections = {
		"Processor Webhook": {
			"main": [[{"node": "Normalize Envelope", "type": "main", "index": 0}]],
		},
		"Normalize Envelope": {
			"main": [[{"node": "Normalize and Triage", "type": "main", "index": 0}]],
		},
		"Normalize and Triage": {
			"main": [[{"node": "Route AI", "type": "main", "index": 0}]],
		},
		"Route AI": {
			"main": [
				[
					{"node": "Information Extractor", "type": "main", "index": 0},
					{"node": "Merge AI Output", "type": "main", "index": 0},
				],
				[
					{"node": "Build Deterministic Payload", "type": "main", "index": 0},
				],
			],
		},
		"Build Deterministic Payload": {
			"main": [[{"node": "Post Deterministic Candidate", "type": "main", "index": 0}]],
		},
		"Post Deterministic Candidate": {
			"main": [[{"node": "Respond Deterministic", "type": "main", "index": 0}]],
		},
		"Google Gemini Chat Model": {
			"ai_languageModel": [[{"node": "Information Extractor", "type": "ai_languageModel", "index": 0}]],
		},
		"Information Extractor": {
			"main": [[{"node": "Merge AI Output", "type": "main", "index": 1}]],
		},
		"Merge AI Output": {
			"main": [[{"node": "Build AI Payload", "type": "main", "index": 0}]],
		},
		"Build AI Payload": {
			"main": [[{"node": "Post AI Candidate", "type": "main", "index": 0}]],
		},
		"Post AI Candidate": {
			"main": [[{"node": "Respond AI", "type": "main", "index": 0}]],
		},
	}

	return workflow(
		"8SwJvATnProcV100",
		"Email Ingestion :: Candidate Processor V1",
		"Common processor for Gmail, Outlook and replay flows. Applies cheap deterministic triage first, uses Information Extractor only for ambiguous/manual emails, and posts candidates to the despesas backend.",
		nodes,
		connections,
		version_id="6d7d0be1-76ee-4575-a7ef-c5949df74434",
		trigger_count=1,
	)


def replay_workflow() -> dict:
	nodes = [
		webhook_node("215bbf98-d321-434c-964c-a4bf5687a37e", "Replay Webhook", "email-ingestion-replay-v1", [-760, 160]),
		code_node("dc8413d4-7d23-4241-9c22-74b7ecf11d9d", "Build Replay Candidate", [-520, 160], REPLAY_SAMPLE_CODE),
		code_node("8cd618df-953f-4d3f-a218-5358d78f70ef", "Call Candidate Processor", [-260, 160], FORWARD_TO_PROCESSOR_CODE),
		respond_node("8f7708f9-2e4f-4622-9ff2-308655789aa8", "Respond Replay", [0, 160]),
	]

	connections = {
		"Replay Webhook": {
			"main": [[{"node": "Build Replay Candidate", "type": "main", "index": 0}]],
		},
		"Build Replay Candidate": {
			"main": [[{"node": "Call Candidate Processor", "type": "main", "index": 0}]],
		},
		"Call Candidate Processor": {
			"main": [[{"node": "Respond Replay", "type": "main", "index": 0}]],
		},
	}

	return workflow(
		"8SwJvATnReplayV1",
		"Email Ingestion :: Replay Harness V1",
		"Replay and smoke-test entrypoint for synthetic or copied real emails. Reuses the common processor webhook and allows backend URL/token overrides in the request body for local validation.",
		nodes,
		connections,
		version_id="7ed0bb0c-625c-4ca5-9d0e-c6c79254f930",
		trigger_count=1,
	)


def gmail_workflow() -> dict:
	nodes = [
		gmail_trigger_node("c98d90bb-f59f-4985-be59-f47d70507d73", [-760, 160]),
		code_node("9a3485e8-a5ec-4b9b-b64d-45f09ddc2128", "Prepare Gmail Envelope", [-480, 160], GMAIL_PREPARE_CODE),
		code_node("2c18f640-d458-4e7f-971f-2da95514bc9e", "Send To Candidate Processor", [-200, 160], FORWARD_TO_PROCESSOR_CODE),
		code_node("00aaf278-e3c5-433f-a0e3-34f44d754dc5", "Map Gmail Mailbox Action", [80, 160], MAILBOX_ACTION_CODE),
		code_node("15f45e70-71b6-4fd9-b899-3cb7b3b61bf1", "Plan Gmail Mailbox Action", [340, 160], GMAIL_MAILBOX_PLAN_CODE),
		gmail_action_node(
			"60f5710f-a0fd-4f00-9b9f-01ad8d1b27fc",
			"Load Gmail Labels",
			[620, 160],
			{
				"resource": "label",
				"operation": "getAll",
			},
		),
		code_node("6a6e188c-9f6c-43ff-82b5-0d1cfeefca1f", "Resolve Gmail Label ID", [900, 160], GMAIL_RESOLVE_LABEL_ID_CODE),
		code_node("2ef802ab-b8b0-492d-9339-1d92161f18e7", "Filter Gmail Label Targets", [1180, 160], FILTER_GMAIL_LABEL_TARGETS_CODE),
		gmail_action_node(
			"4b0d90dd-b56d-4af5-a2cc-d3beea412cd4",
			"Add Gmail Label",
			[1460, 160],
			{
				"resource": "message",
				"operation": "addLabels",
				"messageId": "={{ $json.mailbox.messageId }}",
				"labelIds": "={{ [$json.gmailLabelId] }}",
			},
		),
		gmail_action_node(
			"b0fcf727-b695-4a31-bc29-449ddf9fe31d",
			"Mark Gmail As Read",
			[1740, 160],
			{
				"resource": "message",
				"operation": "removeLabels",
				"messageId": "={{ $json.id || $json.messageId || $json.mailbox?.messageId || '' }}",
				"labelIds": "={{ ['UNREAD'] }}",
			},
		),
	]

	connections = {
		"Gmail Trigger": {
			"main": [[{"node": "Prepare Gmail Envelope", "type": "main", "index": 0}]],
		},
		"Prepare Gmail Envelope": {
			"main": [[{"node": "Send To Candidate Processor", "type": "main", "index": 0}]],
		},
		"Send To Candidate Processor": {
			"main": [[{"node": "Map Gmail Mailbox Action", "type": "main", "index": 0}]],
		},
		"Map Gmail Mailbox Action": {
			"main": [[{"node": "Plan Gmail Mailbox Action", "type": "main", "index": 0}]],
		},
		"Plan Gmail Mailbox Action": {
			"main": [[{"node": "Load Gmail Labels", "type": "main", "index": 0}]],
		},
		"Load Gmail Labels": {
			"main": [[{"node": "Resolve Gmail Label ID", "type": "main", "index": 0}]],
		},
		"Resolve Gmail Label ID": {
			"main": [[{"node": "Filter Gmail Label Targets", "type": "main", "index": 0}]],
		},
		"Filter Gmail Label Targets": {
			"main": [[{"node": "Add Gmail Label", "type": "main", "index": 0}]],
		},
		"Add Gmail Label": {
			"main": [[{"node": "Mark Gmail As Read", "type": "main", "index": 0}]],
		},
	}

	return workflow(
		"8SwJvATnGmailV10",
		"Email Ingestion :: Gmail Trigger V1",
		"Gmail polling workflow. Uses deterministic Gmail search filters and forwards only normalized envelope data to the common processor. Remains unpublished until Gmail credentials and n8n variables are configured.",
		nodes,
		connections,
		version_id="50f970dd-0d18-4c7e-92e7-c413efc935f8",
		trigger_count=1,
	)


def outlook_workflow() -> dict:
	nodes = [
		outlook_trigger_node("aa0c178e-f417-4668-8652-0da7842d1378", [-760, 160]),
		code_node("bed7c118-af17-4c28-bdc9-e0adf6a6a7fa", "Prepare Outlook Envelope", [-480, 160], OUTLOOK_PREPARE_CODE),
		code_node("9a6c9fd0-7b35-4a4e-b888-9a66c0d4e6d5", "Send To Candidate Processor", [-200, 160], FORWARD_TO_PROCESSOR_CODE),
		code_node("99466bbf-6e09-4554-b8dd-55c7d77c2f59", "Map Outlook Mailbox Action", [80, 160], MAILBOX_ACTION_CODE),
		code_node("0d5f60e8-3695-4616-9391-143c7a50340c", "Plan Outlook Mailbox Action", [340, 160], OUTLOOK_MAILBOX_PLAN_CODE),
		outlook_action_node(
			"97cb5ec4-30ce-4511-a638-bf6d8017da19",
			"Load Outlook Folders",
			[620, 160],
			{
				"resource": "folder",
				"operation": "getAll",
				"returnAll": True,
				"filters": {},
				"options": {},
			},
		),
		code_node("85f14725-b59d-4123-8c65-6187da4cab4d", "Resolve Outlook Folder ID", [900, 160], OUTLOOK_RESOLVE_FOLDER_ID_CODE),
		code_node("d2460c37-f9a1-489b-ae85-c7a263a3c4d8", "Filter Outlook Move Targets", [1180, 160], FILTER_OUTLOOK_MOVE_TARGETS_CODE),
		outlook_action_node(
			"1d1aef95-112a-49a2-a6ca-4c9c63d0ea38",
			"Move Outlook Message",
			[1460, 160],
			{
				"resource": "message",
				"operation": "move",
				"messageId": "={{ $json.mailbox.messageId }}",
				"folderId": "={{ $json.outlookTargetFolderId }}",
			},
		),
	]

	connections = {
		"Microsoft Outlook Trigger": {
			"main": [[{"node": "Prepare Outlook Envelope", "type": "main", "index": 0}]],
		},
		"Prepare Outlook Envelope": {
			"main": [[{"node": "Send To Candidate Processor", "type": "main", "index": 0}]],
		},
		"Send To Candidate Processor": {
			"main": [[{"node": "Map Outlook Mailbox Action", "type": "main", "index": 0}]],
		},
		"Map Outlook Mailbox Action": {
			"main": [[{"node": "Plan Outlook Mailbox Action", "type": "main", "index": 0}]],
		},
		"Plan Outlook Mailbox Action": {
			"main": [[{"node": "Load Outlook Folders", "type": "main", "index": 0}]],
		},
		"Load Outlook Folders": {
			"main": [[{"node": "Resolve Outlook Folder ID", "type": "main", "index": 0}]],
		},
		"Resolve Outlook Folder ID": {
			"main": [[{"node": "Filter Outlook Move Targets", "type": "main", "index": 0}]],
		},
		"Filter Outlook Move Targets": {
			"main": [[{"node": "Move Outlook Message", "type": "main", "index": 0}]],
		},
	}

	return workflow(
		"8SwJvATnOutlkV10",
		"Email Ingestion :: Outlook Trigger V1",
		"Reusable Microsoft Outlook polling workflow for Outlook, Hotmail and Live accounts. Normalizes the envelope and forwards it to the common processor. Remains unpublished until Outlook credentials and n8n variables are configured.",
		nodes,
		connections,
		version_id="176ad337-b18d-4b76-80ca-e3df8ea5ca5d",
		trigger_count=1,
	)


def mailbox_bootstrap_workflow() -> dict:
	nodes = [
		webhook_on_received_node("43c4dbf6-1fd8-4b19-9bb8-0b64e01f68fd", "Bootstrap Webhook", "email-ingestion-mailbox-bootstrap-v1", [-1000, 240]),
		gmail_action_node(
			"44d20674-4a7b-4b67-aefc-7132e1f9af9f",
			"Load Gmail Labels",
			[-720, 120],
			{
				"resource": "label",
				"operation": "getAll",
			},
		),
		code_node("b6330ab8-d553-4320-b7c8-27b36ad0b24d", "Find Missing Gmail Labels", [-440, 120], GMAIL_LABEL_BOOTSTRAP_CODE),
		gmail_action_node(
			"c1649c67-7ce0-455d-af71-d0f859f4e4bb",
			"Create Gmail Label",
			[-160, 120],
			{
				"resource": "label",
				"operation": "create",
				"name": "={{ $json.labelName }}",
				"options": {},
			},
		),
		outlook_action_node(
			"0d5d1a55-c198-4f42-ad9d-36f1453e0df6",
			"Load Outlook Folders",
			[-720, 360],
			{
				"resource": "folder",
				"operation": "getAll",
				"returnAll": True,
				"filters": {},
				"options": {},
			},
		),
		code_node("0c36af3a-c9df-48f7-b3a3-16c2938fe2bc", "Find Missing Outlook Folders", [-440, 360], OUTLOOK_FOLDER_BOOTSTRAP_CODE),
		outlook_action_node(
			"b92a5996-6885-484c-81a7-85ad488b8cdb",
			"Create Outlook Folder",
			[-160, 360],
			{
				"resource": "folder",
				"operation": "create",
				"displayName": "={{ $json.folderName }}",
				"options": {},
			},
		),
	]

	connections = {
		"Bootstrap Webhook": {
			"main": [
				[
					{"node": "Load Gmail Labels", "type": "main", "index": 0},
					{"node": "Load Outlook Folders", "type": "main", "index": 0},
				],
			],
		},
		"Load Gmail Labels": {
			"main": [[{"node": "Find Missing Gmail Labels", "type": "main", "index": 0}]],
		},
		"Find Missing Gmail Labels": {
			"main": [[{"node": "Create Gmail Label", "type": "main", "index": 0}]],
		},
		"Load Outlook Folders": {
			"main": [[{"node": "Find Missing Outlook Folders", "type": "main", "index": 0}]],
		},
		"Find Missing Outlook Folders": {
			"main": [[{"node": "Create Outlook Folder", "type": "main", "index": 0}]],
		},
	}

	return workflow(
		"8SwJvATnMailBootstrap1",
		"Email Ingestion :: Mailbox Bootstrap V1",
		"Manual bootstrap for Gmail labels and Outlook folders used by mailboxAction markings in the email ingestion V1 flows.",
		nodes,
		connections,
		version_id="0ebc07a8-4f24-4e74-8ea2-62076d67da12",
		trigger_count=1,
	)


def outlook_live_sender_workflow() -> dict:
	nodes = [
		webhook_last_node("18832bdf-4f64-4b6c-8e87-d81dd647fbac", "Webhook", "email-ingestion-live-send-v1", [-420, 260]),
		code_node("73ebc85f-8933-4b9d-9850-7e9f3b89995f", "Build Messages", [-180, 260], LIVE_SENDER_CODE),
		outlook_send_message_node("8d254a2e-9a43-4331-a111-d7484cbec10e", "Send via Outlook", [80, 260]),
	]

	connections = {
		"Webhook": {
			"main": [[{"node": "Build Messages", "type": "main", "index": 0}]],
		},
		"Build Messages": {
			"main": [[{"node": "Send via Outlook", "type": "main", "index": 0}]],
		},
	}

	return workflow(
		"8SwJvATnOutSend02",
		"Email Ingestion :: Outlook Webhook Sender",
		"Utility webhook that sends a tagged batch of real emails through the configured Outlook account to validate the live email-ingestion pipeline end to end.",
		nodes,
		connections,
		version_id="66bd5dd5-5b61-4382-9ea1-124ed9f9f7e9",
		trigger_count=1,
	)


def main() -> None:
	OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
	workflows = {
		"email-ingestion-candidate-processor-v1.json": processor_workflow(),
		"email-ingestion-replay-harness-v1.json": replay_workflow(),
		"email-ingestion-gmail-trigger-v1.json": gmail_workflow(),
		"email-ingestion-outlook-trigger-v1.json": outlook_workflow(),
		"email-ingestion-mailbox-bootstrap-v1.json": mailbox_bootstrap_workflow(),
		"email-ingestion-outlook-live-sender-v1.json": outlook_live_sender_workflow(),
	}
	for filename, payload in workflows.items():
		(OUTPUT_DIR / filename).write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n")


if __name__ == "__main__":
	main()
