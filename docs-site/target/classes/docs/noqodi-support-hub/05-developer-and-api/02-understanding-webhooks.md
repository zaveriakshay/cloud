[< Back to Support Hub](../index.md)

# Understanding and Using Webhooks

Webhooks are one of the most important tools for a reliable payment integration. They are automated messages sent from PayPro to your server that notify you about events in your account.

### Why Use Webhooks?
Instead of constantly asking our API "Did this payment succeed yet?", you can set up a webhook. When the payment succeeds (or fails, or is refunded), our server will instantly send a message to your server with the update.

This is essential for automating actions like:
* Fulfilling an order after a successful payment.
* Sending a confirmation email to the customer.
* Updating your database with a failed payment status.
* Logging a refund.

### How to Set Up a Webhook

1.  **Create a Webhook Endpoint on Your Server:**
    This is a publicly accessible URL on your application that is designed to receive incoming HTTP POST requests from PayPro. For example: `https://your-domain.com/paypro-webhook-handler`.

2.  **Add the Endpoint in Your Developer Dashboard:**
    * Log in to your PayPro **Developer Dashboard**.
    * Go to the **"Webhooks"** section.
    * Click **"Add endpoint"**.
    * Paste your server's webhook URL into the field.
    * Select the specific events you want to be notified about (e.g., `payment.succeeded`, `payment.failed`). It's best practice to only subscribe to the events your application needs.

    ![Screenshot of adding a webhook endpoint in the Developer Dashboard.](https://placehold.co/800x500/E8E8E8/2E2E2E?text=Add+Webhook+Endpoint)

3.  **Secure Your Webhook (Crucial):**
    Anyone could potentially send fake data to your endpoint. To prevent this, you must verify that the webhook actually came from PayPro.
    * When you create an endpoint, we will provide you with a **Webhook Signing Secret**.
    * Every webhook request we send includes a special `PayPro-Signature` in its header.
    * Your code must use your signing secret to verify this signature. If the signature is invalid, discard the request. Our official API libraries have built-in functions to help with this verification.

**Never trust a webhook payload without verifying its signature first.**