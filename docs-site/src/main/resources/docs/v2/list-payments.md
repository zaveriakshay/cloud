# List Payments

> **GET** `/v2/payments`

Retrieve a comprehensive list of all payments made through your account. This endpoint is essential for reconciliation, reporting, and displaying transaction history to your users.

## 🎯 Overview

The List Payments endpoint provides a paginated and filterable view of your payment history. Results are always returned in reverse chronological order, with the most recent payments appearing first.

**Key Features:**
- 🔢 **Pagination**: Easily navigate through large sets of data using `limit` and `offset`.
- 🔍 **Advanced Filtering**: Drill down into your data by filtering on status, creation date, customer, and more.
- 📈 **Reporting Ready**: The structured response is perfect for feeding into internal dashboards and financial reports.

---

## 🔐 Authentication

All requests must include a valid API key in the Authorization header:


---

## 📋 Request Details

### Query Parameters

| Parameter | Type | Required | Description | Default |
|---|---|---|---|---|
| `limit` | integer | ❌ Optional | A limit on the number of payments to return. | `10` |
| `offset` | integer | ❌ Optional | The number of payments to skip for pagination. | `0` |
| `status` | string | ❌ Optional | Filter payments by their status. | - |
| `created_after` | string | ❌ Optional | Filter for payments created after this ISO 8601 timestamp. | - |
| `created_before` | string | ❌ Optional | Filter for payments created before this ISO 8601 timestamp. | - |
| `customer_id` | string | ❌ Optional | Only return payments associated with this customer ID. | - |

---

## 📤 Response Details

### Success Response (200 OK)


### Response Fields

| Field | Type | Description |
|---|---|---|
| `data` | array | An array of Payment objects. |
| `pagination` | object | An object containing pagination information. |

---

## ⚠️ Error Responses

### 400 Bad Request
**When:** Invalid parameters provided (e.g., non-integer `limit`).

### 401 Unauthorized
**When:** Invalid or missing API key.
