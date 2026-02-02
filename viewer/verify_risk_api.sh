#!/bin/bash

# JSON Payload matching the user request
JSON='{
    "header": {
        "callerSystem": "173471-1",
        "dbBusinessline": "DWS",
        "crrmVersion": "2.0",
        "requestID": "test",
        "requestTimeStamp": "2025-07-30 09:48:30"
    },
    "clientRiskRatingRequest": [
        {
            "clientDetails": {
                "defenceRevenue": null,
                "recordID": "00001497165",
                "clientAdoptionCountry": "DE",
                "smeAssessment": "N",
                "smeRiskAssessment": "",
                "additionalRule": [
                    {
                        "ruleType": "SRF",
                        "question": "ADVERSE_MEDIA",
                        "response": "Y"
                    }
                ]
            },
            "entityRiskType": {
                "typeKYCLegalEntityCode": "NP4"
            },
            "industryRiskType": {
                "occupationCode": [
                    "00101"
                ]
            },
            "geoRiskType": {
                "relatedParty": [
                    {
                        "relatedPartyElementValues": [],
                        "relatedPartyElement": "LR Domicile"
                    }
                ],
                "partyAccount": [
                    {
                        "countryOfNationality": [
                            "DE"
                        ],
                        "originOfFunds": [
                            "DE"
                        ],
                        "dateOfResidence": [
                            "20080515"
                        ],
                        "addressType": {
                            "postalAddress": [],
                            "clientDomicile": "DE"
                        }
                    }
                ]
            },
            "productRiskType": [
                {
                    "productCode": "OAP1"
                }
            ],
            "channelRiskType": {
                "channelCode": "CHN05"
            }
        }
    ]
}'

echo "Sending Request to http://localhost:8080/api/risk/calculate..."
curl -v -X POST http://localhost:8080/api/risk/calculate \
     -H "Content-Type: application/json" \
     -d "$JSON" | json_pp

echo "\nDone."
