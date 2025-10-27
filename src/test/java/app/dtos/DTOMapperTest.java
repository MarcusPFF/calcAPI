package app.dtos;

import app.entities.Calculation;
import app.entities.User;
import app.security.enums.Role;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DTOMapperTest {

    @Test
    void toUserDTOShouldConvertUser() {
        User user = new User();
        user.setId(1);
        user.setUsername("testUser");
        user.setRole(Role.ADMIN);

        UserDTO dto = DTOMapper.toUserDTO(user);

        assertNotNull(dto);
        assertEquals(1, dto.getId());
        assertEquals("testUser", dto.getUsername());
        assertEquals(Role.ADMIN, dto.getRole());
    }

    @Test
    void toUserDTOShouldReturnNullForNullUser() {
        UserDTO dto = DTOMapper.toUserDTO(null);
        assertNull(dto);
    }

    @Test
    void toCalculationDTOShouldConvertCalculation() {
        User user = new User();
        user.setId(1);
        user.setUsername("testUser");
        user.setRole(Role.GUEST);

        Calculation calc = new Calculation();
        calc.setId(1);
        calc.setNum1(5);
        calc.setNum2(10);
        calc.setResult(15);
        calc.setOperation("ADD");
        calc.setTimestamp(LocalDateTime.now());
        calc.setUser(user);

        CalculationDTO dto = DTOMapper.toCalculationDTO(calc);

        assertNotNull(dto);
        assertEquals(1, dto.getId());
        assertEquals(5, dto.getNum1());
        assertEquals(10, dto.getNum2());
        assertEquals(15, dto.getResult());
        assertEquals("ADD", dto.getOperation());
        assertEquals("testUser", dto.getUsername());
        assertNotNull(dto.getTimestamp());
    }

    @Test
    void toCalculationDTOShouldReturnNullForNullCalculation() {
        CalculationDTO dto = DTOMapper.toCalculationDTO(null);
        assertNull(dto);
    }

    @Test
    void toCalculationDTOShouldHandleNullUser() {
        Calculation calc = new Calculation();
        calc.setId(1);
        calc.setNum1(5);
        calc.setNum2(10);
        calc.setResult(15);
        calc.setOperation("ADD");
        calc.setTimestamp(LocalDateTime.now());
        calc.setUser(null);

        CalculationDTO dto = DTOMapper.toCalculationDTO(calc);
        assertNotNull(dto);
        assertNull(dto.getUsername());
    }
}

