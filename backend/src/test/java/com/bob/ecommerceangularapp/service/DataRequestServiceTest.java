package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dao.AbandonedCartRepository;
import com.bob.ecommerceangularapp.dao.CustomerRepository;
import com.bob.ecommerceangularapp.dao.DataRequestRepository;
import com.bob.ecommerceangularapp.dao.GiftCardRepository;
import com.bob.ecommerceangularapp.dao.LoyaltyTransactionRepository;
import com.bob.ecommerceangularapp.dao.NewsletterSubscriberRepository;
import com.bob.ecommerceangularapp.dao.ReferralRepository;
import com.bob.ecommerceangularapp.dao.ReturnRequestRepository;
import com.bob.ecommerceangularapp.dao.SavedAddressRepository;
import com.bob.ecommerceangularapp.dao.SavedPaymentMethodRepository;
import com.bob.ecommerceangularapp.dao.StockNotificationRepository;
import com.bob.ecommerceangularapp.dao.WishlistItemRepository;
import com.bob.ecommerceangularapp.dto.DataExportView;
import com.bob.ecommerceangularapp.dto.DataRequestSubmission;
import com.bob.ecommerceangularapp.dto.DataRequestView;
import com.bob.ecommerceangularapp.email.EmailService;
import com.bob.ecommerceangularapp.entity.AbandonedCart;
import com.bob.ecommerceangularapp.entity.Address;
import com.bob.ecommerceangularapp.entity.Customer;
import com.bob.ecommerceangularapp.entity.DataRequest;
import com.bob.ecommerceangularapp.entity.GiftCard;
import com.bob.ecommerceangularapp.entity.LoyaltyTransaction;
import com.bob.ecommerceangularapp.entity.NewsletterSubscriber;
import com.bob.ecommerceangularapp.entity.Order;
import com.bob.ecommerceangularapp.entity.Referral;
import com.bob.ecommerceangularapp.entity.ReturnRequest;
import com.bob.ecommerceangularapp.entity.SavedAddress;
import com.bob.ecommerceangularapp.entity.SavedPaymentMethod;
import com.bob.ecommerceangularapp.entity.StockNotification;
import com.bob.ecommerceangularapp.entity.WishlistItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests (mocked repositories/services) for the self-service export/erasure flow
 * (roadmap #24): the token lifecycle, the tenant-switching helper that lets an emailed link (which
 * carries no {@code X-Tenant-Id}) still operate on the right tenant's data, and the erasure's
 * delete-outright vs. anonymize-and-retain split.
 */
class DataRequestServiceTest {

    private static final Long TENANT_ID = 7L;
    private static final String EMAIL = "person@example.com";

    private final DataRequestRepository dataRequestRepository = mock(DataRequestRepository.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final SavedAddressRepository savedAddressRepository = mock(SavedAddressRepository.class);
    private final SavedPaymentMethodRepository savedPaymentMethodRepository = mock(SavedPaymentMethodRepository.class);
    private final WishlistItemRepository wishlistItemRepository = mock(WishlistItemRepository.class);
    private final StockNotificationRepository stockNotificationRepository = mock(StockNotificationRepository.class);
    private final AbandonedCartRepository abandonedCartRepository = mock(AbandonedCartRepository.class);
    private final LoyaltyTransactionRepository loyaltyTransactionRepository = mock(LoyaltyTransactionRepository.class);
    private final ReferralRepository referralRepository = mock(ReferralRepository.class);
    private final ReturnRequestRepository returnRequestRepository = mock(ReturnRequestRepository.class);
    private final GiftCardRepository giftCardRepository = mock(GiftCardRepository.class);
    private final NewsletterSubscriberRepository newsletterSubscriberRepository = mock(NewsletterSubscriberRepository.class);
    private final PrivacyConsentService privacyConsentService = mock(PrivacyConsentService.class);
    private final PaymentMethodService paymentMethodService = mock(PaymentMethodService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final EmailService emailService = mock(EmailService.class);

    private final DataRequestService service = new DataRequestService(
            dataRequestRepository, customerRepository, savedAddressRepository, savedPaymentMethodRepository,
            wishlistItemRepository, stockNotificationRepository, abandonedCartRepository,
            loyaltyTransactionRepository, referralRepository, returnRequestRepository, giftCardRepository,
            newsletterSubscriberRepository, privacyConsentService, paymentMethodService, auditLogService,
            emailService, "http://localhost:8585", 48);

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT_ID);
        when(emailService.isEnabled()).thenReturn(true);
        // Every list-returning collaborator defaults to "nothing found" so each test only stubs
        // what it actually cares about.
        when(savedAddressRepository.findByEmailIgnoreCaseAndTenantIdOrderByDefaultAddressDescIdDesc(anyString(), anyLong()))
                .thenReturn(List.of());
        when(savedPaymentMethodRepository.findByEmailIgnoreCaseAndTenantIdOrderByDefaultMethodDescIdDesc(anyString(), anyLong()))
                .thenReturn(List.of());
        when(wishlistItemRepository.findByEmailAndTenantId(anyString(), anyLong())).thenReturn(List.of());
        when(stockNotificationRepository.findByEmailIgnoreCaseAndTenantId(anyString(), anyLong())).thenReturn(List.of());
        when(abandonedCartRepository.findByEmailIgnoreCaseAndTenantId(anyString(), anyLong())).thenReturn(List.of());
        when(loyaltyTransactionRepository.findByCustomerEmailIgnoreCaseAndTenantId(anyString(), anyLong())).thenReturn(List.of());
        when(referralRepository.findByRefereeEmailIgnoreCaseAndTenantId(anyString(), anyLong())).thenReturn(List.of());
        when(returnRequestRepository.findByCustomerEmailIgnoreCaseAndTenantIdOrderByDateCreatedDesc(anyString(), anyLong())).thenReturn(List.of());
        when(giftCardRepository.findByRecipientEmailIgnoreCaseAndTenantId(anyString(), anyLong())).thenReturn(List.of());
        when(dataRequestRepository.findByEmailIgnoreCaseAndTenantIdOrderByIdDesc(anyString(), anyLong())).thenReturn(List.of());
        when(privacyConsentService.historyForEmail(anyString(), anyLong())).thenReturn(List.of());
        when(privacyConsentService.policyVersion()).thenReturn("2026-09-01");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ------------------------------------------------------------------ submit

    @Test
    void submit_unknownRequestType_throws() {
        assertThatThrownBy(() -> service.submit(new DataRequestSubmission(EMAIL, "BOGUS")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submit_newRequest_savesMailsAndAudits() {
        when(dataRequestRepository.findByEmailIgnoreCaseAndRequestTypeAndStatusAndTenantId(
                eq(EMAIL), eq(DataRequest.TYPE_EXPORT), eq(DataRequest.STATUS_PENDING_VERIFICATION), eq(TENANT_ID)))
                .thenReturn(List.of());
        when(dataRequestRepository.save(any(DataRequest.class))).thenAnswer(inv -> {
            DataRequest r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        DataRequestView view = service.submit(new DataRequestSubmission(EMAIL, "export"));

        assertThat(view.requestType()).isEqualTo(DataRequest.TYPE_EXPORT);
        assertThat(view.status()).isEqualTo(DataRequest.STATUS_PENDING_VERIFICATION);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendDataRequestVerification(eq(EMAIL), eq(DataRequest.TYPE_EXPORT), urlCaptor.capture(), eq(48));
        assertThat(urlCaptor.getValue()).startsWith("http://localhost:8585/api/privacy/confirm?token=");

        verify(auditLogService).record(eq(null), eq("DATA_REQUEST_SUBMIT"), eq("DataRequest"), anyString(), anyString());
    }

    @Test
    void submit_reusesAnOpenUnexpiredRequestInsteadOfDuplicating() {
        DataRequest existing = DataRequest.builder().id(9L).tenantId(TENANT_ID).email(EMAIL)
                .requestType(DataRequest.TYPE_ERASURE).status(DataRequest.STATUS_PENDING_VERIFICATION)
                .token("tok").expiresAt(new Date(System.currentTimeMillis() + 60_000)).build();
        when(dataRequestRepository.findByEmailIgnoreCaseAndRequestTypeAndStatusAndTenantId(
                eq(EMAIL), eq(DataRequest.TYPE_ERASURE), eq(DataRequest.STATUS_PENDING_VERIFICATION), eq(TENANT_ID)))
                .thenReturn(List.of(existing));

        DataRequestView view = service.submit(new DataRequestSubmission(EMAIL, "ERASURE"));

        assertThat(view.id()).isEqualTo(9L);
        verify(dataRequestRepository, never()).save(any());
        verify(emailService, never()).sendDataRequestVerification(anyString(), anyString(), anyString(), anyInt());
    }

    // ----------------------------------------------------------------- confirm

    @Test
    void confirm_unknownToken_returnsEmpty() {
        when(dataRequestRepository.findByToken("nope")).thenReturn(Optional.empty());
        assertThat(service.confirm("nope")).isEmpty();
    }

    @Test
    void confirm_pendingToken_marksVerified() {
        DataRequest request = pendingRequest();
        when(dataRequestRepository.findByToken("tok")).thenReturn(Optional.of(request));
        when(dataRequestRepository.save(any(DataRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<DataRequest> confirmed = service.confirm("tok");

        assertThat(confirmed).isPresent();
        assertThat(confirmed.get().getStatus()).isEqualTo(DataRequest.STATUS_VERIFIED);
        assertThat(confirmed.get().getVerifiedAt()).isNotNull();
    }

    @Test
    void confirm_expiredToken_expiresInPlaceAndReturnsEmpty() {
        DataRequest request = DataRequest.builder().id(1L).tenantId(TENANT_ID).email(EMAIL)
                .requestType(DataRequest.TYPE_EXPORT).status(DataRequest.STATUS_PENDING_VERIFICATION)
                .token("tok").expiresAt(new Date(System.currentTimeMillis() - 1000)).build();
        when(dataRequestRepository.findByToken("tok")).thenReturn(Optional.of(request));
        when(dataRequestRepository.save(any(DataRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.confirm("tok")).isEmpty();
        assertThat(request.getStatus()).isEqualTo(DataRequest.STATUS_EXPIRED);
    }

    @Test
    void confirm_alreadyCompletedToken_isNoLongerUsable() {
        DataRequest request = DataRequest.builder().id(1L).tenantId(TENANT_ID).email(EMAIL)
                .requestType(DataRequest.TYPE_EXPORT).status(DataRequest.STATUS_COMPLETED)
                .token("tok").expiresAt(new Date(System.currentTimeMillis() + 60_000)).build();
        when(dataRequestRepository.findByToken("tok")).thenReturn(Optional.of(request));

        assertThat(service.confirm("tok")).isEmpty();
    }

    // ------------------------------------------------------------------ export

    @Test
    void export_wrongRequestType_returnsEmpty() {
        DataRequest request = verifiedRequest(DataRequest.TYPE_ERASURE);
        when(dataRequestRepository.findByToken("tok")).thenReturn(Optional.of(request));
        assertThat(service.export("tok")).isEmpty();
    }

    @Test
    void export_notYetVerified_returnsEmpty() {
        DataRequest request = pendingRequest();
        when(dataRequestRepository.findByToken("tok")).thenReturn(Optional.of(request));
        assertThat(service.export("tok")).isEmpty();
    }

    @Test
    void export_bindsToTheRequestsTenantNotTheAmbientOne_andRestoresItAfterward() {
        // Simulate the real scenario: the subject clicked a link from their inbox, so whatever
        // tenant TenantResolutionFilter fell back to (no header) is bound ambiently — here a
        // different tenant than the one the request actually belongs to.
        Long ambientTenant = 999L;
        TenantContext.set(ambientTenant);

        DataRequest request = verifiedRequest(DataRequest.TYPE_EXPORT);
        when(dataRequestRepository.findByToken("tok")).thenReturn(Optional.of(request));
        when(dataRequestRepository.save(any(DataRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.findByEmailAndTenantId(EMAIL, TENANT_ID)).thenReturn(null);
        when(newsletterSubscriberRepository.findByEmailAndTenantId(EMAIL, TENANT_ID)).thenReturn(null);

        Optional<DataExportView> bundle = service.export("tok");

        assertThat(bundle).isPresent();
        assertThat(bundle.get().email()).isEqualTo(EMAIL);
        assertThat(bundle.get().orders()).isEmpty();
        assertThat(bundle.get().notIncluded()).isNotEmpty();
        // Every collaborator was queried with the REQUEST's tenant, not the ambient ("999") one.
        verify(wishlistItemRepository).findByEmailAndTenantId(EMAIL, TENANT_ID);
        // The ambient context is restored once the export finishes.
        assertThat(TenantContext.currentTenantId()).isEqualTo(ambientTenant);

        assertThat(request.getStatus()).isEqualTo(DataRequest.STATUS_COMPLETED);
        assertThat(request.getCompletedAt()).isNotNull();
        assertThat(request.getResultSummary()).contains("Exported 0 order(s)");
        verify(auditLogService).record(eq(null), eq("DATA_REQUEST_EXPORT"), eq("DataRequest"), anyString(), anyString());
    }

    @Test
    void export_includesTheCustomersOrdersAndProfile() {
        DataRequest request = verifiedRequest(DataRequest.TYPE_EXPORT);
        when(dataRequestRepository.findByToken("tok")).thenReturn(Optional.of(request));
        when(dataRequestRepository.save(any(DataRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer customer = new Customer();
        customer.setFirstName("Ada");
        customer.setLastName("Lovelace");
        customer.setEmail(EMAIL);
        customer.setLoyaltyPoints(50);
        customer.setLifetimePoints(200);
        Order order = new Order();
        order.setId(1L);
        order.setOrderTrackingNumber("TRK-1");
        order.setTotalPrice(new java.math.BigDecimal("19.99"));
        order.setOrderItems(new HashSet<>());
        customer.setOrders(new HashSet<>(List.of(order)));
        when(customerRepository.findByEmailAndTenantId(EMAIL, TENANT_ID)).thenReturn(customer);
        when(newsletterSubscriberRepository.findByEmailAndTenantId(EMAIL, TENANT_ID)).thenReturn(null);

        DataExportView bundle = service.export("tok").orElseThrow();

        assertThat(bundle.profile()).isNotNull();
        assertThat(bundle.profile().firstName()).isEqualTo("Ada");
        assertThat(bundle.orders()).hasSize(1);
        assertThat(bundle.orders().get(0).orderTrackingNumber()).isEqualTo("TRK-1");
    }

    // ------------------------------------------------------------------- erase

    @Test
    void erase_wrongRequestType_returnsEmpty() {
        DataRequest request = verifiedRequest(DataRequest.TYPE_EXPORT);
        when(dataRequestRepository.findByToken("tok")).thenReturn(Optional.of(request));
        assertThat(service.erase("tok")).isEmpty();
    }

    @Test
    void erase_notYetVerified_returnsEmpty() {
        DataRequest request = pendingRequestOfType(DataRequest.TYPE_ERASURE);
        when(dataRequestRepository.findByToken("tok")).thenReturn(Optional.of(request));
        assertThat(service.erase("tok")).isEmpty();
    }

    @Test
    void erase_noDataAtAll_returnsGenericNoDataMessage() {
        DataRequest request = verifiedRequest(DataRequest.TYPE_ERASURE);
        when(dataRequestRepository.findByToken("tok")).thenReturn(Optional.of(request));
        when(dataRequestRepository.save(any(DataRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.findByEmailAndTenantId(EMAIL, TENANT_ID)).thenReturn(null);
        when(newsletterSubscriberRepository.findByEmailAndTenantId(EMAIL, TENANT_ID)).thenReturn(null);
        when(privacyConsentService.deleteForEmail(EMAIL, TENANT_ID)).thenReturn(0);

        String summary = service.erase("tok").orElseThrow();

        assertThat(summary).isEqualTo("No personal data was found for this address.");
    }

    @Test
    void erase_deletesConvenienceDataOutright_andAnonymizesRetainedFinancialRecords() {
        DataRequest request = verifiedRequest(DataRequest.TYPE_ERASURE);
        when(dataRequestRepository.findByToken("tok")).thenReturn(Optional.of(request));
        when(dataRequestRepository.save(any(DataRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        WishlistItem wishlistItem = new WishlistItem();
        when(wishlistItemRepository.findByEmailAndTenantId(EMAIL, TENANT_ID)).thenReturn(List.of(wishlistItem));

        StockNotification watch = new StockNotification();
        when(stockNotificationRepository.findByEmailIgnoreCaseAndTenantId(EMAIL, TENANT_ID)).thenReturn(List.of(watch));

        AbandonedCart cart = new AbandonedCart();
        when(abandonedCartRepository.findByEmailIgnoreCaseAndTenantId(EMAIL, TENANT_ID)).thenReturn(List.of(cart));

        SavedAddress address = new SavedAddress();
        when(savedAddressRepository.findByEmailIgnoreCaseAndTenantIdOrderByDefaultAddressDescIdDesc(EMAIL, TENANT_ID))
                .thenReturn(List.of(address));

        SavedPaymentMethod card = new SavedPaymentMethod();
        card.setId(42L);
        when(savedPaymentMethodRepository.findByEmailIgnoreCaseAndTenantIdOrderByDefaultMethodDescIdDesc(EMAIL, TENANT_ID))
                .thenReturn(List.of(card));

        NewsletterSubscriber subscriber = new NewsletterSubscriber();
        when(newsletterSubscriberRepository.findByEmailAndTenantId(EMAIL, TENANT_ID)).thenReturn(subscriber);

        when(privacyConsentService.deleteForEmail(EMAIL, TENANT_ID)).thenReturn(2);

        LoyaltyTransaction points = new LoyaltyTransaction();
        points.setId(5L);
        points.setCustomerEmail(EMAIL);
        when(loyaltyTransactionRepository.findByCustomerEmailIgnoreCaseAndTenantId(EMAIL, TENANT_ID)).thenReturn(List.of(points));

        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setId(6L);
        returnRequest.setCustomerEmail(EMAIL);
        when(returnRequestRepository.findByCustomerEmailIgnoreCaseAndTenantIdOrderByDateCreatedDesc(EMAIL, TENANT_ID))
                .thenReturn(List.of(returnRequest));

        Referral referral = new Referral();
        referral.setId(8L);
        referral.setRefereeEmail(EMAIL);
        when(referralRepository.findByRefereeEmailIgnoreCaseAndTenantId(EMAIL, TENANT_ID)).thenReturn(List.of(referral));

        GiftCard giftCard = GiftCard.builder().id(3L).recipientEmail(EMAIL).balance(new java.math.BigDecimal("10.00")).build();
        when(giftCardRepository.findByRecipientEmailIgnoreCaseAndTenantId(EMAIL, TENANT_ID)).thenReturn(List.of(giftCard));

        Customer customer = new Customer();
        customer.setId(11L);
        customer.setEmail(EMAIL);
        customer.setFirstName("Ada");
        customer.setLastName("Lovelace");
        customer.setLoyaltyPoints(50);
        customer.setLifetimePoints(200);
        customer.setReferralCode("ADA123");
        customer.setUnsubscribeToken("unsub-token");
        Address shipping = new Address();
        shipping.setStreet("1 Infinite Loop");
        shipping.setCity("Cupertino");
        Order order = new Order();
        order.setShippingAddress(shipping);
        customer.setOrders(new HashSet<>(List.of(order)));
        when(customerRepository.findByEmailAndTenantId(EMAIL, TENANT_ID)).thenReturn(customer);

        String summary = service.erase("tok").orElseThrow();

        verify(wishlistItemRepository).deleteAll(List.of(wishlistItem));
        verify(stockNotificationRepository).deleteAll(List.of(watch));
        verify(abandonedCartRepository).deleteAll(List.of(cart));
        verify(savedAddressRepository).deleteAll(List.of(address));
        verify(paymentMethodService).delete(EMAIL, 42L);
        verify(newsletterSubscriberRepository).delete(subscriber);
        verify(privacyConsentService).deleteForEmail(EMAIL, TENANT_ID);

        assertThat(points.getCustomerEmail()).startsWith("erased-").endsWith("@erased.invalid");
        assertThat(returnRequest.getCustomerEmail()).startsWith("erased-").endsWith("@erased.invalid");
        assertThat(referral.getRefereeEmail()).startsWith("erased-").endsWith("@erased.invalid");
        assertThat(giftCard.getRecipientEmail()).isNull();
        assertThat(giftCard.getBalance()).isEqualByComparingTo("10.00"); // store credit is never confiscated

        assertThat(customer.getFirstName()).isEqualTo("Erased");
        assertThat(customer.getLastName()).isEqualTo("Erased");
        assertThat(customer.getEmail()).startsWith("erased-").endsWith("@erased.invalid");
        assertThat(customer.isNewsletterSubscribed()).isFalse();
        assertThat(customer.getUnsubscribeToken()).isNull();
        assertThat(customer.getReferralCode()).isNull();
        assertThat(customer.getLoyaltyPoints()).isZero();
        assertThat(customer.getLifetimePoints()).isZero();
        assertThat(shipping.getStreet()).isNull();
        assertThat(shipping.getCity()).isEqualTo("Cupertino"); // jurisdiction kept for tax records

        assertThat(request.getStatus()).isEqualTo(DataRequest.STATUS_COMPLETED);
        assertThat(request.getResultSummary()).isEqualTo(summary);
        assertThat(summary).contains("1 order retained as financial records");
        verify(auditLogService).record(eq(null), eq("DATA_REQUEST_ERASURE"), eq("DataRequest"), anyString(), eq(summary));
    }

    // ----------------------------------------------------------------- helpers

    private static DataRequest pendingRequest() {
        return pendingRequestOfType(DataRequest.TYPE_EXPORT);
    }

    private static DataRequest pendingRequestOfType(String type) {
        return DataRequest.builder().id(1L).tenantId(TENANT_ID).email(EMAIL).requestType(type)
                .status(DataRequest.STATUS_PENDING_VERIFICATION).token("tok")
                .expiresAt(new Date(System.currentTimeMillis() + 60_000)).build();
    }

    private static DataRequest verifiedRequest(String type) {
        return DataRequest.builder().id(1L).tenantId(TENANT_ID).email(EMAIL).requestType(type)
                .status(DataRequest.STATUS_VERIFIED).token("tok")
                .expiresAt(new Date(System.currentTimeMillis() + 60_000)).build();
    }
}
