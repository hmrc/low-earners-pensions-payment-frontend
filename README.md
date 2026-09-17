
# Low Earners Pensions Payment Frontend

The frontend service for the low-earners-pensions-payment (LEPP) project.

LEPP ensures that PAYE individuals who earn up to or below the tax-free limit, and contribute to a NPA pension scheme, are able to view their entitlement, provide their bank details and view the status of their payment in order to receive tax-relief top-up payments easily and smoothly to ensure all paying into either a RAS or NPA pension receive the same tax relief.

## Dependencies
| Service                      | Link                                                 |
|------------------------------|------------------------------------------------------|
| low-earners-pensions-payment | https://github.com/hmrc/low-earners-pensions-payment |
| bank-account-reputation      | https://github.com/hmrc/bank-account-reputation      |


### Endpoints used

| Service                      | HTTP Method | Route                                                 | Purpose                                           |
|------------------------------|-------------|-------------------------------------------------------|---------------------------------------------------|
| low-earners-pensions-payment | POST        | /low-earners-pensions-payment/accept-payment          | Calls out backend to accept the payment           |
| low-earners-pensions-payment | GET         | /low-earners-pensions-payment/get-payment-details     | Calls out backend to retrieve the payment details |
| Personal Tax Account         | GET         | /personal-account                                     | Redirects to PTA service                          |

## Running the service

Service Manager: sm2 -start LEPP_ALL

Port: 7503

Link: http://localhost:7503/accept-your-low-earners-pension-payment

Enrolment PTA: `HMRC-PT` `AA111111A` (local and Staging environments only)


## Tests and prototype
[View the prototype here](https://lepp-prototype-74ec22ca1ed6.herokuapp.com/)

| Repositories      | Link                                                                   |
|-------------------|------------------------------------------------------------------------|
| Journey tests     | https://github.com/hmrc/low-earners-pensions-payment-ui-tests          |
| Performance tests | https://github.com/hmrc/low-earners-pensions-payment-performance-tests |
| Prototype         | https://github.com/hmrc/low-earners-anomaly-prototype                  |
