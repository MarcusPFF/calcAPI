package app.dtos;

import app.entities.Calculation;
import app.entities.User;

public class DTOMapper {

    /*
    konverter fra en Calculation entity (fra databasen via DAO) til en CalculationDTO (til at sende til klienten)
    Controller  ←→  Service  ←→  DAO  ←→  Database
         ↑
    DTO’er bruges her (Controller/Service)

    to"xx"DTO() = fra entity → DTO

     */

    public static UserDTO toUserDTO(User user) {
        if (user == null) return null;
        return new UserDTO(user.getId(), user.getUsername(), user.getRole());
    }

    public static CalculationDTO toCalculationDTO(Calculation calc) {
        if (calc == null) return null;
        String username = calc.getUser() != null ? calc.getUser().getUsername() : null;
        return new CalculationDTO(
                calc.getId(),
                calc.getNum1(),
                calc.getNum2(),
                calc.getResult(),
                calc.getOperation(),
                calc.getTimestamp(),
                username
        );
    }
}