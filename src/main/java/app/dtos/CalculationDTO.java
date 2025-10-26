package app.dtos;

import app.entities.Calculation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalculationDTO {
    private int id;
    private double num1;
    private double num2;
    private double result;
    private String operation;
    private LocalDateTime timestamp;
    private String username;

    public CalculationDTO(Calculation calc) {
        this.id = calc.getId();
        this.num1 = calc.getNum1();
        this.num2 = calc.getNum2();
        this.result = calc.getResult();
        this.operation = calc.getOperation();
        this.timestamp = calc.getTimestamp();
        if (calc.getUser() != null) {
            this.username = calc.getUser().getUsername();
        }    }
}