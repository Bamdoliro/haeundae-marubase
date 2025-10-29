package com.bamdoliro.maru.domain.form.domain.type;

import com.bamdoliro.maru.shared.property.EnumProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@Getter
@RequiredArgsConstructor
public enum FormType implements EnumProperty {
    REGULAR("일반전형", Category.REGULAR, Category.REGULAR),

    NATIONAL_VETERANS("국가보훈대상자", Category.SPECIAL, Category.EQUAL_OPPORTUNITY),
    NATIONAL_BASIC_LIVING("국민기초생활수급권자", Category.SPECIAL, Category.EQUAL_OPPORTUNITY),
    ONE_PARENT("한부모가족 보호대상자", Category.SPECIAL, Category.EQUAL_OPPORTUNITY),
    NEAR_POVERTY("차상위계층", Category.SPECIAL, Category.EQUAL_OPPORTUNITY),
    LOWER_MIDDLE("차차상위계층", Category.SPECIAL, Category.EQUAL_OPPORTUNITY),
    PRINCIPAL_RECOMMENDATION("학교장 추천", Category.SPECIAL, Category.EQUAL_OPPORTUNITY),
    SUPERINTENDENT_RECOMMENDATION("교육감 추천", Category.SPECIAL, Category.EQUAL_OPPORTUNITY),

    MULTICULTURAL("다문화가족자녀", Category.SPECIAL, Category.SOCIETY_DIVERSITY),
    FROM_NORTH_KOREA("북한이탈청소년", Category.SPECIAL, Category.SOCIETY_DIVERSITY),
    SPECIAL_EDUCATION_STUDENT("특수교육대상자", Category.SPECIAL, Category.SOCIETY_DIVERSITY),
    CHILD_WELFARE_FACILITY("아동복지시설 보호학생", Category.SPECIAL, Category.SOCIETY_DIVERSITY),
    TEEN_HOUSEHOLDER("소년·소녀가장", Category.SPECIAL, Category.SOCIETY_DIVERSITY),
    GRANDFAMILY("조손 가정 자녀", Category.SPECIAL, Category.SOCIETY_DIVERSITY),
    DISABLED_PARENT("장애인의 자녀", Category.SPECIAL, Category.SOCIETY_DIVERSITY),
    FALLEN_HERO("순직 군경·소방관 등 자녀", Category.SPECIAL, Category.SOCIETY_DIVERSITY),
    MULTI_CHILDREN("다자녀가정자녀", Category.SPECIAL, Category.SOCIETY_DIVERSITY),
    NON_STATUTORY_ONE_PARENT("한부모가족 자녀(비법정)", Category.SPECIAL, Category.SOCIETY_DIVERSITY),
    WELFARE_FACILITY_WORKER("복지시설 운영자·종사자 자녀", Category.SPECIAL, Category.SOCIETY_DIVERSITY),
    PUBLIC_SERVANT("경찰·군인·소방공무원 자녀", Category.SPECIAL, Category.SOCIETY_DIVERSITY),
    STREET_CLEANER("환경미화원 자녀", Category.SPECIAL, Category.SOCIETY_DIVERSITY),
    DEPLOYED_SOLDIER("해외파병군인 자녀", Category.SPECIAL, Category.SOCIETY_DIVERSITY),
    INTANGIBLE_CULTURAL_HERITAGE("무형문화재 보유자 자녀", Category.SPECIAL, Category.SOCIETY_DIVERSITY),
    POSTMAN("우편집배원 자녀",Category.SPECIAL, Category.SOCIETY_DIVERSITY),
    SAILOR("선원 자녀", Category.SPECIAL, Category.SOCIETY_DIVERSITY),

    SPECIAL_ADMISSION("특례입학대상자", Category.SUPERNUMERARY, Category.SPECIAL_ADMISSION),
    NATIONAL_VETERANS_EDUCATION("국가보훈대상자 중 교육지원대상자", Category.SUPERNUMERARY, Category.NATIONAL_VETERANS_EDUCATION),
    ;

    private final String description;
    private final Category mainCategory;
    private final Category subCategory;

    @Getter
    @RequiredArgsConstructor
    public enum Category implements EnumProperty {
        // Main Category
        REGULAR("일반전형"),
        SPECIAL("사회통합전형"),
        SUPERNUMERARY("정원 외 전형"),

        // Sub Category
        EQUAL_OPPORTUNITY("기회균등전형"),
        SOCIETY_DIVERSITY("사회다양성전형"),
        NATIONAL_VETERANS_EDUCATION("국가보훈대상자 중 교육지원대상자녀"),
        SPECIAL_ADMISSION("특례입학대상자")
        ;

        private final String description;

        public static Category valueOfDescription(String description) {
            for (Category category : Category.values()) {
                if (Objects.equals(category.getDescription(), description)) {
                    return category;
                }
            }
            throw new IllegalArgumentException("No matching constant for [" + description + "]");
        }
    }

    public boolean isRegular() {
        return mainCategory == Category.REGULAR;
    }

    public boolean isRegularOrSupernumerary() {
        return isRegular() || isSupernumerary();
    }

    public boolean isSpecial() {
        return mainCategory == Category.SPECIAL;
    }

    public boolean isEqualOpportunity() {
        return subCategory == Category.EQUAL_OPPORTUNITY;
    }

    public boolean isSocietyDiversity() {
        return subCategory == Category.SOCIETY_DIVERSITY;
    }

    public boolean isSupernumerary() {
        return mainCategory == Category.SUPERNUMERARY;
    }

    public boolean isNationalVeteransEducation() {
        return this == NATIONAL_VETERANS_EDUCATION;
    }

    public boolean isSpecialAdmission() {
        return this == SPECIAL_ADMISSION;
    }

    public boolean categoryEquals(Category category) {
        return mainCategory.equals(category)
                || (Objects.nonNull(subCategory) && subCategory.equals(category));
    }

    public FormType.Category getCategory() {
        return mainCategory;
    }
}
