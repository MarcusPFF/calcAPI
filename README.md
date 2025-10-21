# Calculator API

Calculator API er et simpelt backend-projekt i Java (Javalin + JPA), hvor brugere kan lave beregninger via REST endpoints, og resultater gemmes i databasen.

Roller og adgang:
- **USER**: `/api/add`, `/api/subtract`
- **ADMIN**: plus adgang til `/api/multiply`, `/api/divide`

Sikkerhed:
- Endpoints er beskyttet med **JWT**.
- Ved **oprettelse af bruger** hashes passwordet med BCrypt, og **kun hash** gemmes i databasen.

Data:
- Hver beregning gemmes sammen med den bruger, der udførte den, inkl. num1, num2, operation, resultat og timestamp.
