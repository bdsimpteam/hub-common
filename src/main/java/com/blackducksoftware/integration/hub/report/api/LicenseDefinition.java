package com.blackducksoftware.integration.hub.report.api;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;

public class LicenseDefinition {

    private static final String AND = " AND ";

    private static final String OR = " OR ";

    // this is what KB might answer with
    private static final String MAPPING_PENDING = "Mapping Pending";

    private static final String UNKNOWN = "UNKNOWN";

    private static final String OPEN_PARENTHESIS = "(";

    private static final String CLOSED_PARENTHESIS = ")";

    private final UUID licenseId;

    private final String discoveredAs;

    private final String name;

    private final String spdxId;

    private final String ownership;

    private final String codeSharing;

    private final LicenseType licenseType;

    private final List<LicenseDefinition> licenses;

    public enum LicenseType {
        CONJUNCTIVE, DISJUNCTIVE;
    }

    /**
     * to be thrown when we want to stop the recursive processing and unwind the stack
     */
    @SuppressWarnings("serial")
    private static final class LicenseCustomException extends RuntimeException {

    }

    public LicenseDefinition(UUID licenseId,
            String discoveredAs, String name, String spdxId,
            String ownership, String codeSharing,
            LicenseType licenseType, List<LicenseDefinition> licenses) {
        this.licenseId = licenseId;
        this.discoveredAs = discoveredAs;
        this.name = name;
        this.spdxId = spdxId;
        this.ownership = ownership;
        this.codeSharing = codeSharing;
        this.licenseType = licenseType;
        this.licenses = licenses;

    }

    public UUID getLicenseId() {
        return licenseId;
    }

    public String getDiscoveredAs() {
        return discoveredAs;
    }

    public String getName() {
        return name;
    }

    public List<LicenseDefinition> getLicenses() {
        return licenses;
    }

    public String getSpdxId() {
        return spdxId;
    }

    public String getOwnership() {
        return ownership;
    }

    public String getCodeSharing() {
        return codeSharing;
    }

    public LicenseType getLicenseType() {
        return licenseType;
    }

    /**
     * This method is supposed to be called by JSON serializer only
     */
    public String getLicenseDisplay() {
        String result = "";
        try {
            result = computeLicenseDisplay();
        } catch (LicenseCustomException e) {
            result = "UNKNOWN";
        }

        // this is the case when we have a single license and it's "Mapping Pending"
        if (MAPPING_PENDING.equals(result) || Strings.isNullOrEmpty(result)) {
            return UNKNOWN;
        }
        return result;
    }

    /**
     * this is considered a "dirty license display", it needs some
     * further processing
     */
    private String computeLicenseDisplay() {
        if (licenses.isEmpty()) {
            return getName();
        } else {
            String operator = getOperator(this);
            Collection<String> result = Collections2.transform(licenses, new Function<LicenseDefinition, String>() {
                @Override
                public String apply(LicenseDefinition licenseDef) {
                    return licenseDef.computeLicenseDisplay();
                }
            });

            /**
             * AND 'Mapping Pending' => 'UNKNOWN' and high-risk
             * OR 'Mapping Pending' => discard 'Mapping Pending' at all
             */
            // result.contains("") is needed for Mapping Pending OR Mapping Pending
            if (result.contains(MAPPING_PENDING) || result.contains("")) {
                if (AND.equals(operator) && result.contains(MAPPING_PENDING)) {
                    throw new LicenseCustomException();
                } else {
                    result = Collections2.filter(result, new Predicate<String>() {
                        @Override
                        public boolean apply(String input) {
                            return !MAPPING_PENDING.equals(input) && !Strings.isNullOrEmpty(input);
                        }
                    });
                }
            }
            return result.size() > 1 ? OPEN_PARENTHESIS + Joiner.on(operator).join(result) + CLOSED_PARENTHESIS
                    : Joiner.on(operator).join(result);
        }
    }

    private String getOperator(LicenseDefinition ld) {
        return ld.getLicenseType() == LicenseType.CONJUNCTIVE ? AND : OR;
    }

}
