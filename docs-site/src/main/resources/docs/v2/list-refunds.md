# List Refunds

> **GET** `/v2/refunds`

Retrieve a list of all refunds that have been created on your account. This endpoint is useful for accounting, reconciliation, and customer service workflows.

## 🎯 Overview

The List Refunds endpoint provides a paginated list of your refund objects. You can filter the list by the original payment or by the refund's status. The results are always returned in reverse chronological order, with the most recently created refunds appearing first.

**Key Features:**
- 🔢 **Pagination**: Easily navigate through your refund history using `limit` and `offset`.
- 🔍 **Filtering**: Find specific refunds by filtering on the associated `payment_id` or `status`.
- 📈 **Reconciliation**: Provides the data needed for financial reporting and balancing your books.

---

## 🔐 Authentication

All requests must include a valid API key in the Authorization header:

---

## 📋 Request Details

### Query Parameters

| Parameter | Type | Required | Description | Default |
|---|---|---|---|---|
| `limit` | integer | ❌ Optional | A limit on the number of refunds to return (1-100). | `10` |
| `offset` | integer | ❌ Optional | The number of refunds to skip for pagination. | `0` |
| `payment_id` | string | ❌ Optional | Filter refunds by the original payment ID. | - |
| `status` | string | ❌ Optional | Filter refunds by their status (`pending`, `succeeded`, `failed`, `cancelled`). | - |

---

## 📤 Response Details

### Success Response (200 OK)
