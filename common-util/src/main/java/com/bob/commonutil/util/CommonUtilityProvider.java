package com.bob.commonutil.util;

import com.bob.commonutil.exception.ManualValidationException;
import com.bob.commonutil.model.SalaryBreakUpModel;
import com.bob.db.entity.CandidateCompensationEntity;
import com.bob.db.entity.CandidateProfileEntity;
import com.bob.db.entity.WorkExperienceEntity;
import com.ibm.icu.text.RuleBasedNumberFormat;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class CommonUtilityProvider {

    public  String buildFullName(CandidateProfileEntity profile) {
        if (profile == null) {
            return "";
        }
        return Stream.of(profile.getFirstName(), profile.getMiddleName(), profile.getLastName())
                .filter(name -> name != null && !name.trim().isEmpty())
                .collect(Collectors.joining(" "));
    }

    public int calculateTotalMonths(List<WorkExperienceEntity> workExperiences) {
        if (workExperiences == null) return 0;
        return workExperiences.stream()
                .mapToInt(WorkExperienceEntity::getMonthsOfExp)
                .sum();
    }

    public  String formatToIndianCurrency(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }


        String plainStr = amount.toPlainString();


        String[] parts = plainStr.split("\\.");
        String integerPart = parts[0];


        // 3. Handle numbers smaller than 1000 (no commas needed)
        if (integerPart.length() <= 3) {
            return integerPart;
        }


        String lastThree = integerPart.substring(integerPart.length() - 3);
        String otherNumbers = integerPart.substring(0, integerPart.length() - 3);

        // Regex to insert commas every 2 digits
        String formattedInteger = otherNumbers.replaceAll("\\B(?=(\\d{2})+(?!\\d))", ",") + "," + lastThree;

        return formattedInteger;
    }

    public String buildPercentageOrCgpa(BigDecimal value) {
        if(value == null || value.compareTo(BigDecimal.ZERO) <= 0) return null;
        if (value != null && value.compareTo(BigDecimal.TEN) <=0) {return  value.setScale(2, RoundingMode.DOWN).toString() + " CGPA";}
        return value.setScale(2, RoundingMode.DOWN).toString() + "%";
    }

     public LocalTime getReportingTime(LocalTime currTime) {
        if(currTime == null) return null;
        return currTime.minusMinutes(30);
     }

     public boolean hasThreeDayGap(LocalDate date1,LocalDate date2){
         long gap = ChronoUnit.DAYS.between(date1, date2);
         return gap>=AppConstants.ZERO && gap <=AppConstants.TWO;
     }


     public String getAgeString(LocalDate cutoffDate,LocalDate dateOfBirth){
         if(cutoffDate== null || dateOfBirth == null) {return null;};
         Period period = Period.between(dateOfBirth, cutoffDate.plusDays(AppConstants.ONE));
         return period.getYears() + " " + AppConstants.YEARS + " " + period.getMonths() + " " + AppConstants.MONTHS + " " + period.getDays() + " " + AppConstants.DAYS;

     }

     public String getYesorNo(boolean value) {

         return value ? AppConstants.YES : AppConstants.NO;
     }

    private boolean isMetroCity(String city) {
        if (city == null || city.isBlank()) return false;
        String lowercaseCity = city.toLowerCase();
        return AppConstants.METRO_CITIES.contains(city);
    }


    public SalaryBreakUpModel calculateCompensationBreakdown(CandidateCompensationEntity compensationEntity, String postingCity) {
        // Return null if entity or fixed pay is not present
        if (compensationEntity == null || compensationEntity.getFixedPay() == null) {
            return null;
        }

        BigDecimal fixedPay = compensationEntity.getFixedPay(); // Total Fixed Pay (X)
        BigDecimal variablePay = compensationEntity.getVariablePay() != null ? compensationEntity.getVariablePay() : BigDecimal.ZERO; // Variable Pay (Y)
        BigDecimal twelve = new BigDecimal("12");

        // 1. Total CTC = Fixed Pay (X) + Variable Pay (Y)
        BigDecimal totalCtc = fixedPay.add(variablePay);
        BigDecimal monthlyFixedPay = fixedPay.divide(twelve, 2, RoundingMode.HALF_UP);

        // 2. Basic Pay (A) = Fixed Pay (X) / 2
        BigDecimal annualBasic = fixedPay.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        BigDecimal monthlyBasic = annualBasic.divide(twelve, 2, RoundingMode.HALF_UP);

        // 3. House Rent Allowance (B) calculation based on city
        // 50% of Basic Pay for Metro cities, 40% for all other cities
        BigDecimal hraMultiplier = isMetroCity(postingCity) ? new BigDecimal("0.50") : new BigDecimal("0.40");
        BigDecimal annualHra = annualBasic.multiply(hraMultiplier).setScale(2, RoundingMode.HALF_UP);
        BigDecimal monthlyHra = annualHra.divide(twelve, 2, RoundingMode.HALF_UP);

        // 4. Medical Allowance (D) = Fixed at 15,000/- Per Annum
        BigDecimal annualMedical = new BigDecimal("15000.00");
        BigDecimal monthlyMedical = annualMedical.divide(twelve, 2, RoundingMode.HALF_UP); // 1,250/-

        // 5. Entertainment Allowance (E) = Fixed at 60,000/- Per Annum
        BigDecimal annualEntertainment = new BigDecimal("60000.00");
        BigDecimal monthlyEntertainment = annualEntertainment.divide(twelve, 2, RoundingMode.HALF_UP); // 5,000/-

        // 6. Supplementary Allowance (C) = X - A - B - D - E
        BigDecimal sumDeductions = annualBasic
                .add(annualHra)
                .add(annualMedical)
                .add(annualEntertainment);

        if(fixedPay.compareTo(sumDeductions) < 0){
            throw new ManualValidationException("Sum of HRA,Medical,Entertainment allowances exceed Total Fixed Pay");
        }
        BigDecimal annualSupplementary = fixedPay.subtract(sumDeductions);

        BigDecimal monthlySupplementary = annualSupplementary.divide(twelve, 2, RoundingMode.HALF_UP);

        // Build and return the Data Model using the Builder Pattern
        return SalaryBreakUpModel.builder()
                .totalCtc(totalCtc)
                .variablePay(variablePay)
                .annualTotalFixed(fixedPay)
                .monthlyTotalFixed(monthlyFixedPay)
                .annualBasic(annualBasic)
                .monthlyBasic(monthlyBasic)
                .annualHra(annualHra)
                .monthlyHra(monthlyHra)
                .annualMedical(annualMedical)
                .monthlyMedical(monthlyMedical)
                .annualEntertainment(annualEntertainment)
                .monthlyEntertainment(monthlyEntertainment)
                .annualSupplementary(annualSupplementary)
                .monthlySupplementary(monthlySupplementary)
                .build();
    }



    public String convertNumberToWordsInIndianSystem(BigDecimal number,String language) {
        RuleBasedNumberFormat formatter = null;

        if (language.equalsIgnoreCase("hindi")) {
            formatter = new RuleBasedNumberFormat(
                    new Locale("hi", "IN"),
                    RuleBasedNumberFormat.SPELLOUT);
        } else {
            formatter = new RuleBasedNumberFormat(
                    new Locale("en", "IN"),
                    RuleBasedNumberFormat.SPELLOUT);
        }

        return formatter.format(number);
    }

    public String maskMobileNumber(String mobileNumber) {

        String lastFourDigits = mobileNumber.substring(mobileNumber.length() - 4);
        return AppConstants.MOBILE_NUMBER_MASK + lastFourDigits;
    }

    public String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "-";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email; // Invalid email format
        }

        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (username.length() <= 2) {
            return "*".repeat(username.length()) + domain;
        }

        return username.charAt(0)
                + "*".repeat(username.length() - 2)
                + username.charAt(username.length() - 1)
                + domain;
    }
}
