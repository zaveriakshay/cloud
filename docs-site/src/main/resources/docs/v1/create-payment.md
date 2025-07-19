# Create Payment

> **POST** `/v1/payments`

Transform your payment dreams into reality with our powerful payment creation endpoint. This is where the magic happens - turning customer intent into successful transactions across multiple payment methods and currencies.

## 🎯 Overview

The Create Payment endpoint is your gateway to processing payments from customers worldwide. Whether it's a simple credit card transaction, a sophisticated digital wallet payment, or a complex multi-currency conversion, this endpoint handles it all with grace and reliability.

**Key Features:**
- 🌍 **Multi-Currency Support**: Process payments in 150+ currencies
- 💳 **Multiple Payment Methods**: Credit cards, digital wallets, bank transfers, and BNPL
- 🔒 **Advanced Security**: PCI DSS Level 1 compliance with 3D Secure authentication
- ⚡ **Real-time Processing**: Sub-second response times for most transactions
- 🔄 **Idempotency**: Prevent duplicate charges with idempotency keys

---

## 🔐 Authentication

All requests must include a valid API key in the Authorization header:

```http
Authorization: Bearer sk_live_your_api_key_here
```

---

## 📋 Request Details

### Headers

| Header | Type | Required | Description |
|--------|------|----------|-------------|
| `Authorization` | string | ✅ **Required** | Bearer token with your API key |
| `Content-Type` | string | ✅ **Required** | Must be `application/json` |
| `Idempotency-Key` | string | ❌ Optional | Unique key to prevent duplicate requests (max 255 chars) |

### Request Body

| Field | Type | Required | Description | Default |
|-------|------|----------|-------------|---------|
| `amount` | integer | ✅ **Required** | Amount in smallest currency unit (cents, paise, etc.) | - |
| `currency` | string | ✅ **Required** | 3-letter ISO currency code (USD, EUR, GBP, etc.) | - |
| `payment_method` | object | ✅ **Required** | Payment method details | - |
| `description` | string | ❌ Optional | Payment description (max 500 chars) | `null` |
| `capture` | boolean | ❌ Optional | Whether to capture immediately or authorize only | `true` |
| `merchant_info` | object | ❌ Optional | Merchant information for the transaction | - |
| `customer` | object | ❌ Optional | Customer details | - |
| `shipping_address` | object | ❌ Optional | Shipping address information | - |
| `metadata` | object | ❌ Optional | Additional key-value pairs for your reference | `{}` |

### Payment Method Object

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | string | ✅ **Required** | One of: `credit_card`, `wallet`, `netbanking`, `bnpl`, `bank_transfer` |
| `card` | object | ❌ Conditional | Required if type is `credit_card` |
| `wallet` | object | ❌ Conditional | Required if type is `wallet` |

### Card Object

| Field | Type | Required | Description | Format |
|-------|------|----------|-------------|---------|
| `card_number` | string | ✅ **Required** | Card number (PAN) | 13-19 digits |
| `expiry_month` | integer | ✅ **Required** | Expiration month | 1-12 |
| `expiry_year` | integer | ✅ **Required** | Expiration year | 4-digit year |
| `cvc` | string | ❌ Optional | Card verification code | 3-4 digits |
| `cardholder_name` | string | ❌ Optional | Name on card | Max 100 chars |

---

## 📤 Response Details

### Success Response (201 Created)

```json
{
  "id": "pay_1234567890abcdef",
  "status": "succeeded",
  "amount": 2500,
  "currency": "USD",
  "description": "Premium subscription payment",
  "created_at": "2024-01-15T10:30:00Z",
  "updated_at": "2024-01-15T10:30:05Z",
  "payment_method": {
    "type": "credit_card",
    "card": {
      "last4": "4242",
      "brand": "visa",
      "exp_month": 12,
      "exp_year": 2025,
      "cardholder_name": "John Doe",
      "country": "US",
      "funding": "credit"
    }
  },
  "customer": {
    "customer_id": "cust_abc123",
    "email": "john@example.com",
    "name": "John Doe"
  },
  "receipt_url": "https://receipts.globalpayments.com/pay_1234567890abcdef",
  "metadata": {
    "order_id": "order_12345",
    "customer_segment": "premium"
  }
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique payment identifier starting with `pay_` |
| `status` | string | Payment status: `succeeded`, `pending`, `failed`, `requires_action`, `requires_capture` |
| `amount` | integer | Payment amount in smallest currency unit |
| `currency` | string | 3-letter ISO currency code |
| `description` | string | Payment description |
| `created_at` | string | ISO 8601 timestamp of payment creation |
| `updated_at` | string | ISO 8601 timestamp of last update |
| `payment_method` | object | Details of the payment method used |
| `customer` | object | Customer information |
| `receipt_url` | string | URL to access payment receipt |
| `metadata` | object | Custom metadata provided in request |

---

## ⚠️ Error Responses

### 400 Bad Request
**When:** Invalid parameters provided

| Error Code | Description | Solution |
|------------|-------------|----------|
| `invalid_amount` | Amount must be positive | Ensure amount is greater than 0 |
| `invalid_currency` | Invalid currency code | Use valid 3-letter ISO currency code |
| `missing_payment_method` | Payment method not provided | Include payment method in request |

### 401 Unauthorized
**When:** Invalid or missing API key

| Error Code | Description | Solution |
|------------|-------------|----------|
| `invalid_api_key` | API key is invalid | Check your API key |
| `api_key_expired` | API key has expired | Generate new API key |

### 402 Payment Required
**When:** Payment cannot be processed

| Error Code | Description | Common Causes