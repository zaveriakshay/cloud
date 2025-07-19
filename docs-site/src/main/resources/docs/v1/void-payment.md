# Void a Payment

> **POST** `/v1/payments/{paymentId}/void`

Cancel a previously authorized but uncaptured payment. This action releases the hold on the customer's funds and prevents the payment from ever being captured.

## 🎯 Overview

Voiding a payment is the correct way to cancel a transaction before it has been finalized (captured). This is a common requirement for orders that are canceled by the customer before fulfillment. Unlike a refund, a voided transaction typically disappears from the customer's statement entirely, as no funds were ever transferred.

**Key Features:**
- ↩️ **Clean Reversal**: Releases the authorization hold on a customer's card.
- 🚫 **Prevents Capture**: Once voided, a payment can no longer be captured.
- 📝 **Reason Tracking**: Add a reason to the void for internal auditing and clarity.

---

## 🔐 Authentication

All requests must include a valid API key in the Authorization header:

---

## 📋 Request Details

### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `paymentId` | string | ✅ **Required** | The unique identifier of the payment to void, starting with `pay_`. |

### Request Body

| Field | Type | Required | Description |
|---|---|---|---|
| `reason` | string | ❌ Optional | A reason for voiding the payment (e.g., `customer_request`). |

---

## 📤 Response Details

### Success Response (200 OK)
The response is a full Payment object, with its status updated to `cancelled`.

### Response Fields
The response fields are identical to those returned when creating a payment.

---

## ⚠️ Error Responses

### 401 Unauthorized
**When:** Invalid or missing API key.

### 404 Not Found
**When:** No payment exists with the provided `paymentId`.

### 422 Unprocessable Entity
**When:** The payment cannot be voided (e.g., it has already been captured or cancelled).