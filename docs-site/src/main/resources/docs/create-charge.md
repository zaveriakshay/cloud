# Create a charge

To charge a credit card or other payment source, you create a `Charge` object. If your API key is in test mode, the charge will be a test charge.

## Arguments

- **amount** `integer` _required_

  A positive integer in the smallest currency unit representing how much to charge.

- **currency** `string` _required_

  Three-letter ISO currency code, in lowercase.

- **source** `string` _optional_

  A payment source to be charged.