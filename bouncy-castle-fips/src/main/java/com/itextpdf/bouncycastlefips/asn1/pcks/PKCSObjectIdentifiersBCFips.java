package com.itextpdf.bouncycastlefips.asn1.pcks;

import com.itextpdf.bouncycastlefips.asn1.ASN1ObjectIdentifierBCFips;
import com.itextpdf.commons.bouncycastle.asn1.IASN1ObjectIdentifier;
import com.itextpdf.commons.bouncycastle.asn1.pkcs.IPKCSObjectIdentifiers;

import java.util.Objects;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;

public class PKCSObjectIdentifiersBCFips implements IPKCSObjectIdentifiers {
    private static final PKCSObjectIdentifiersBCFips INSTANCE = new PKCSObjectIdentifiersBCFips(null);

    private static final ASN1ObjectIdentifierBCFips ID_AA_ETS_SIG_POLICY_ID = new ASN1ObjectIdentifierBCFips(
            PKCSObjectIdentifiers.id_aa_ets_sigPolicyId);

    private static final ASN1ObjectIdentifierBCFips ID_AA_SIGNATURE_TIME_STAMP_TOKEN = new ASN1ObjectIdentifierBCFips(
            PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);

    private static final ASN1ObjectIdentifierBCFips ID_SPQ_ETS_URI =
            new ASN1ObjectIdentifierBCFips(PKCSObjectIdentifiers.id_spq_ets_uri);

    private static final ASN1ObjectIdentifierBCFips ENVELOPED_DATA = new ASN1ObjectIdentifierBCFips(
            PKCSObjectIdentifiers.envelopedData);

    private static final ASN1ObjectIdentifierBCFips DATA = new ASN1ObjectIdentifierBCFips(
            PKCSObjectIdentifiers.data);

    private final PKCSObjectIdentifiers pkcsObjectIdentifiers;

    public PKCSObjectIdentifiersBCFips(PKCSObjectIdentifiers pkcsObjectIdentifiers) {
        this.pkcsObjectIdentifiers = pkcsObjectIdentifiers;
    }

    public static IPKCSObjectIdentifiers getInstance() {
        return INSTANCE;
    }

    public PKCSObjectIdentifiers getPkcsObjectIdentifiers() {
        return pkcsObjectIdentifiers;
    }

    @Override
    public IASN1ObjectIdentifier getIdAaSignatureTimeStampToken() {
        return ID_AA_SIGNATURE_TIME_STAMP_TOKEN;
    }

    @Override
    public IASN1ObjectIdentifier getIdAaEtsSigPolicyId() {
        return ID_AA_ETS_SIG_POLICY_ID;
    }

    @Override
    public IASN1ObjectIdentifier getIdSpqEtsUri() {
        return ID_SPQ_ETS_URI;
    }

    @Override
    public IASN1ObjectIdentifier getEnvelopedData() {
        return ENVELOPED_DATA;
    }

    @Override
    public IASN1ObjectIdentifier getData() {
        return DATA;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PKCSObjectIdentifiersBCFips that = (PKCSObjectIdentifiersBCFips) o;
        return Objects.equals(pkcsObjectIdentifiers, that.pkcsObjectIdentifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pkcsObjectIdentifiers);
    }

    @Override
    public String toString() {
        return pkcsObjectIdentifiers.toString();
    }
}
