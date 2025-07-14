package com.madcamp.moody.mood;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MoodRequestDTO {
    private String date; // "2024-07-14"
    private String mood; // "HAPPY" 등
}
