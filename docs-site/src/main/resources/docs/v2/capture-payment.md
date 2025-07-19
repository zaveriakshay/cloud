# Capture a Payment

> **POST** `/v2/payments/{paymentId}/capture`

Finalize a previously authorized payment. This action moves the funds from the customer's account to yours and is a critical step for payments that use a two-step authorization and capture flow.

## 🎯 Overview

When you create a payment with `capture` set to `false`, the funds are only authorized (held) on the customer's card. The Capture Payment endpoint is used to complete the transaction. This is common in e-commerce scenarios where you only charge the customer once the goods have been shipped.

**Key Features:**
- 💰 **Partial Captures**: Capture less than the originally authorized amount. This is useful if only part of an order can be fulfilled.
- ✅ **Finalize Transactions**: The definitive step to move funds and complete the payment lifecycle.
- 📝 **Descriptive Captures**: Add a description to the capture for clear record-keeping.

---

## 🔐 Authentication

All requests must include a valid API key in the Authorization header:

---

## 📋 Request Details

### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `paymentId` | string | ✅ **Required** | The unique identifier of the payment to capture, starting with `pay_`. |

### Request Body

| Field | Type | Required | Description |
|---|---|---|---|
| `amount` | integer | ❌ Optional | The amount to capture, in the smallest currency unit. If omitted, the full authorized amount is captured. |
| `description` | string | ❌ Optional | An optional description for this capture event. |

---

## 📤 Response Details

### Success Response (200 OK)
The response is a full Payment object, with its status updated to `succeeded`.

### Response Fields
The response fields are identical to those returned when creating a payment.

---

## ⚠️ Error Responses

### 400 Bad Request
**When:** Invalid parameters, such as capturing more than the authorized amount.

### 401 Unauthorized
**When:** Invalid or missing API key.

### 404 Not Found
**When:** No payment exists with the provided `paymentId`.

### 422 Unprocessable Entity
**When:** The payment cannot be captured (e.g., it's not in a `requires_capture` state or has expired).