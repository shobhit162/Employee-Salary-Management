package com.acme.employeemanagement.seed;

import java.math.BigDecimal;
import java.util.List;

/**
 * The shape of the fictional ACME organisation the seeder builds.
 *
 * <p>Salary bands are expressed in each country's own currency at roughly
 * realistic local levels, so that converting them to a single reporting currency
 * is a genuine test of the analytics rather than a no-op.
 */
final class OrganisationProfile {

    private OrganisationProfile() {
    }

    /**
     * @param headcountWeight relative share of the organisation based in this country
     */
    record Country(
            String code,
            String name,
            String currency,
            BigDecimal salaryMultiplier,
            int headcountWeight
    ) {
    }

    /**
     * @param payFactor how this function is paid relative to the country baseline
     */
    record Department(
            String name,
            double payFactor,
            int headcountWeight,
            List<String> jobTitles
    ) {
    }

    /**
     * @param baseSalary  midpoint of the level's band in the base currency (USD)
     * @param spread      fraction of the midpoint the actual salary may vary by
     */
    record Level(
            String name,
            BigDecimal baseSalary,
            double spread,
            int weight
    ) {
    }

    static final List<Country> COUNTRIES = List.of(
            new Country("US", "United States", "USD", new BigDecimal("1.00"), 30),
            new Country("IN", "India", "INR", new BigDecimal("28.00"), 28),
            new Country("GB", "United Kingdom", "GBP", new BigDecimal("0.68"), 10),
            new Country("DE", "Germany", "EUR", new BigDecimal("0.78"), 9),
            new Country("PL", "Poland", "PLN", new BigDecimal("2.10"), 6),
            new Country("SG", "Singapore", "SGD", new BigDecimal("1.20"), 5),
            new Country("AU", "Australia", "AUD", new BigDecimal("1.30"), 4),
            new Country("CA", "Canada", "CAD", new BigDecimal("1.15"), 4),
            new Country("BR", "Brazil", "BRL", new BigDecimal("2.60"), 3),
            new Country("AE", "United Arab Emirates", "AED", new BigDecimal("3.20"), 1)
    );

    static final List<Department> DEPARTMENTS = List.of(
            new Department("Engineering", 1.15, 34, List.of(
                    "Software Engineer",
                    "Senior Software Engineer",
                    "Staff Engineer",
                    "Engineering Manager",
                    "Site Reliability Engineer",
                    "QA Engineer"
            )),
            new Department("Sales", 1.05, 14, List.of(
                    "Account Executive",
                    "Sales Development Representative",
                    "Regional Sales Manager",
                    "Solutions Consultant"
            )),
            new Department("Customer Support", 0.72, 12, List.of(
                    "Support Specialist",
                    "Senior Support Specialist",
                    "Support Team Lead"
            )),
            new Department("Operations", 0.85, 9, List.of(
                    "Operations Analyst",
                    "Operations Manager",
                    "Logistics Coordinator"
            )),
            new Department("Marketing", 0.95, 7, List.of(
                    "Marketing Specialist",
                    "Content Strategist",
                    "Product Marketing Manager"
            )),
            new Department("Finance", 1.02, 6, List.of(
                    "Financial Analyst",
                    "Accountant",
                    "Finance Manager"
            )),
            new Department("Product", 1.12, 6, List.of(
                    "Product Manager",
                    "Senior Product Manager",
                    "Product Designer"
            )),
            new Department("People", 0.88, 5, List.of(
                    "HR Business Partner",
                    "Recruiter",
                    "People Operations Specialist"
            )),
            new Department("Legal", 1.10, 3, List.of(
                    "Legal Counsel",
                    "Contracts Manager"
            )),
            new Department("Data", 1.18, 4, List.of(
                    "Data Analyst",
                    "Data Engineer",
                    "Data Scientist"
            ))
    );

    static final List<Level> LEVELS = List.of(
            new Level("Junior", new BigDecimal("62000"), 0.12, 30),
            new Level("Mid", new BigDecimal("96000"), 0.14, 38),
            new Level("Senior", new BigDecimal("142000"), 0.16, 22),
            new Level("Lead", new BigDecimal("188000"), 0.18, 8),
            new Level("Director", new BigDecimal("255000"), 0.22, 2)
    );

    static final List<String> FIRST_NAMES = List.of(
            "Aarav", "Adaeze", "Adam", "Aisha", "Alejandro", "Alice", "Amara",
            "Amelia", "Ana", "Anders", "Ananya", "Andre", "Anika", "Antoine",
            "Arjun", "Astrid", "Ayaan", "Beatriz", "Ben", "Bianca", "Camila",
            "Carlos", "Caroline", "Chen", "Chloe", "Daniel", "Deepak", "Diego",
            "Divya", "Elena", "Elias", "Emily", "Emma", "Eshan", "Fatima",
            "Felix", "Fiona", "Gabriel", "Grace", "Hannah", "Hassan", "Henry",
            "Hiroshi", "Ibrahim", "Ines", "Isabella", "Ivan", "Jacob", "Jamal",
            "Javier", "Jenna", "Joana", "Jonas", "Julia", "Kabir", "Kai",
            "Karolina", "Katarzyna", "Kavya", "Kenji", "Lars", "Laura", "Leila",
            "Liam", "Lucia", "Lukas", "Maja", "Manon", "Marcus", "Maria",
            "Mateo", "Maya", "Meera", "Mikael", "Mohammed", "Naomi", "Natalia",
            "Nikhil", "Noah", "Nora", "Olivia", "Omar", "Oscar", "Paulo",
            "Priya", "Rafael", "Rahul", "Rania", "Riya", "Rohan", "Rosa",
            "Samuel", "Sanjay", "Sara", "Sofia", "Sven", "Tara", "Thomas",
            "Vikram", "Wei", "Yara", "Zara"
    );

    static final List<String> LAST_NAMES = List.of(
            "Abbott", "Adeyemi", "Ahmed", "Almeida", "Andersson", "Bakker",
            "Barros", "Becker", "Bennett", "Bianchi", "Brennan", "Cabrera",
            "Chandra", "Chen", "Clarke", "Costa", "Dahl", "Das", "Delgado",
            "Desai", "Dubois", "Duarte", "Eriksson", "Farrell", "Fernandes",
            "Fischer", "Fontaine", "Garcia", "Gill", "Gomes", "Grant", "Gupta",
            "Haddad", "Hansen", "Harris", "Hoffmann", "Iyer", "Jain", "Jensen",
            "Johnson", "Kaminski", "Kaur", "Keller", "Khan", "Kowalski",
            "Kumar", "Larsen", "Lefebvre", "Leung", "Lindqvist", "Lopez",
            "Mahmood", "Malik", "Marchetti", "Martins", "Mehta", "Mendes",
            "Meyer", "Mishra", "Moreau", "Murphy", "Nakamura", "Navarro",
            "Nguyen", "Nielsen", "Nowak", "Okafor", "Oliveira", "Owusu",
            "Pandey", "Park", "Patel", "Pereira", "Petrov", "Pillai", "Quinn",
            "Rahman", "Ramirez", "Rao", "Reddy", "Ribeiro", "Richter", "Rossi",
            "Sharma", "Silva", "Singh", "Sinha", "Smith", "Sousa", "Stewart",
            "Suzuki", "Tanaka", "Thakur", "Torres", "Tremblay", "Vargas",
            "Verma", "Walsh", "Weber", "Wong", "Wright", "Yilmaz", "Zhang"
    );
}
