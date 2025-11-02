package com.pneumaliback.www.dto;

import jakarta.validation.constraints.NotNull;
import com.pneumaliback.www.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGenderRequest {
    @NotNull(message = "Le genre est requis")
    private Gender gender;
}
