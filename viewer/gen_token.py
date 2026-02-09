
import jwt
import datetime

# Secret from application.properties
SECRET = "ThisIsA VeryLongAndComplexSecretKeyThatShouldBeEnoughForHS512AlgorithmMakeSureItIsAtLeast64BytesLongToSatisfyTheSecurityRequirement1234567890!_extra_padding_to_be_safe"

def generate_token():
    payload = {
        "sub": "analyst",
        "auth": "ROLE_KYC_ANALYST,MANAGE_CASES", # Matching data.sql analyst roles roughly
        "iat": datetime.datetime.utcnow(),
        "exp": datetime.datetime.utcnow() + datetime.timedelta(hours=1)
    }
    
    token = jwt.encode(payload, SECRET, algorithm="HS512")
    print(token)

if __name__ == "__main__":
    generate_token()
