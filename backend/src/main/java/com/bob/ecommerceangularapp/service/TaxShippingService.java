package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.ShippingMethodRepository;
import com.bob.ecommerceangularapp.dao.TaxRateRepository;
import com.bob.ecommerceangularapp.dto.CouponResponse;
import com.bob.ecommerceangularapp.dto.QuoteRequest;
import com.bob.ecommerceangularapp.dto.QuoteResponse;
import com.bob.ecommerceangularapp.dto.ShippingMethodRequest;
import com.bob.ecommerceangularapp.dto.ShippingMethodView;
import com.bob.ecommerceangularapp.dto.TaxRateRequest;
import com.bob.ecommerceangularapp.entity.ShippingMethod;
import com.bob.ecommerceangularapp.entity.TaxRate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * Authoritative tax + shipping computation. The same {@link #quote} method backs the storefront's
 * live totals (the {@code /api/checkout/quote} endpoint) and the server-side recompute at order time,
 * so what the customer sees and what is charged/recorded can never drift. Also owns the admin CRUD for
 * the rate/method config tables.
 */
@Service
public class TaxShippingService {

    private final TaxRateRepository taxRateRepository;
    private final ShippingMethodRepository shippingMethodRepository;
    private final CouponService couponService;

    public TaxShippingService(TaxRateRepository taxRateRepository,
                              ShippingMethodRepository shippingMethodRepository,
                              CouponService couponService) {
        this.taxRateRepository = taxRateRepository;
        this.shippingMethodRepository = shippingMethodRepository;
        this.couponService = couponService;
    }

    // ---------- storefront ----------

    @Transactional(readOnly = true)
    public List<ShippingMethodView> listShippingMethods() {
        return shippingMethodRepository.findByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(m -> new ShippingMethodView(m.getId(), m.getCode(), m.getName(),
                        m.getBaseRate(), m.getFreeOverThreshold(), m.getEstimatedDays()))
                .toList();
    }

    /**
     * Computes the full totals breakdown: discount (re-validated coupon), shipping (chosen method, free
     * over its threshold), tax (region rate on the discounted merchandise) and the grand total.
     */
    @Transactional(readOnly = true)
    public QuoteResponse quote(QuoteRequest request) {
        BigDecimal subtotal = nz(request.subtotal());

        BigDecimal discount = BigDecimal.ZERO;
        if (request.couponCode() != null && !request.couponCode().isBlank()) {
            CouponResponse coupon = couponService.validate(request.couponCode(), subtotal);
            if (coupon.valid()) {
                discount = nz(coupon.discount());
            }
        }
        BigDecimal discounted = subtotal.subtract(discount).max(BigDecimal.ZERO);

        ShippingMethod method = resolveShippingMethod(request.shippingMethodCode());
        BigDecimal shipping = shippingFor(method, subtotal);

        BigDecimal ratePercent = resolveRatePercent(request.country(), request.state());
        BigDecimal tax = discounted.multiply(ratePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal total = discounted.add(shipping).add(tax).max(BigDecimal.ZERO);
        return new QuoteResponse(money(subtotal), money(discount), money(shipping), money(tax),
                ratePercent, money(total), method == null ? null : method.getCode());
    }

    private ShippingMethod resolveShippingMethod(String code) {
        if (code != null && !code.isBlank()) {
            ShippingMethod m = shippingMethodRepository.findByCode(code).orElse(null);
            if (m != null && m.isActive()) {
                return m;
            }
        }
        // fall back to the cheapest active method so a quote always has a shipping basis
        return shippingMethodRepository.findByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .min(Comparator.comparing(ShippingMethod::getBaseRate))
                .orElse(null);
    }

    private BigDecimal shippingFor(ShippingMethod method, BigDecimal subtotal) {
        if (method == null) {
            return BigDecimal.ZERO;
        }
        if (method.getFreeOverThreshold() != null
                && subtotal.compareTo(method.getFreeOverThreshold()) >= 0) {
            return BigDecimal.ZERO;
        }
        return nz(method.getBaseRate());
    }

    /** Most-specific tax rate for a region: country+state, else country-wide, else 0%. */
    private BigDecimal resolveRatePercent(String country, String state) {
        if (country == null || country.isBlank()) {
            return BigDecimal.ZERO;
        }
        List<TaxRate> rates = taxRateRepository.findByActiveTrue();
        TaxRate countryWide = null;
        for (TaxRate r : rates) {
            if (!country.equalsIgnoreCase(r.getCountry())) {
                continue;
            }
            boolean rateHasState = r.getState() != null && !r.getState().isBlank();
            if (rateHasState && state != null && r.getState().equalsIgnoreCase(state)) {
                return nz(r.getRatePercent()); // exact region match wins
            }
            if (!rateHasState) {
                countryWide = r;
            }
        }
        return countryWide != null ? nz(countryWide.getRatePercent()) : BigDecimal.ZERO;
    }

    // ---------- admin ----------

    @Transactional(readOnly = true)
    public List<TaxRate> listTaxRates() {
        return taxRateRepository.findAll();
    }

    @Transactional
    public TaxRate saveTaxRate(TaxRateRequest req) {
        TaxRate rate = req.id() != null
                ? taxRateRepository.findById(req.id())
                        .orElseThrow(() -> new IllegalArgumentException("Tax rate not found: " + req.id()))
                : new TaxRate();
        rate.setCountry(req.country().trim());
        rate.setState(req.state() == null || req.state().isBlank() ? null : req.state().trim());
        rate.setRatePercent(req.ratePercent());
        rate.setActive(req.active());
        return taxRateRepository.save(rate);
    }

    @Transactional
    public void deleteTaxRate(Long id) {
        if (!taxRateRepository.existsById(id)) {
            throw new IllegalArgumentException("Tax rate not found: " + id);
        }
        taxRateRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ShippingMethod> listAllShippingMethods() {
        return shippingMethodRepository.findAll();
    }

    @Transactional
    public ShippingMethod saveShippingMethod(ShippingMethodRequest req) {
        ShippingMethod method = req.id() != null
                ? shippingMethodRepository.findById(req.id())
                        .orElseThrow(() -> new IllegalArgumentException("Shipping method not found: " + req.id()))
                : new ShippingMethod();
        method.setCode(req.code().trim());
        method.setName(req.name().trim());
        method.setBaseRate(req.baseRate());
        method.setFreeOverThreshold(req.freeOverThreshold());
        method.setEstimatedDays(req.estimatedDays());
        method.setSortOrder(req.sortOrder());
        method.setActive(req.active());
        return shippingMethodRepository.save(method);
    }

    @Transactional
    public void deleteShippingMethod(Long id) {
        if (!shippingMethodRepository.existsById(id)) {
            throw new IllegalArgumentException("Shipping method not found: " + id);
        }
        shippingMethodRepository.deleteById(id);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal money(BigDecimal v) {
        return nz(v).setScale(2, RoundingMode.HALF_UP);
    }
}
