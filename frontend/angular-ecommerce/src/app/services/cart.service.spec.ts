import { CartItem } from '../common/cart-item';
import { Product } from '../common/product';
import { CartService } from './cart.service';

function makeProduct(id: number, price: number): Product {
  return {
    id,
    sku: `SKU-${id}`,
    name: `Product ${id}`,
    description: '',
    unitPrice: price,
    imageUrl: '',
    active: true,
    unitsInStock: 10,
    dateCreated: new Date(),
    lastUpdated: new Date(),
  };
}

describe('CartService', () => {
  let service: CartService;

  beforeEach(() => {
    sessionStorage.clear();
    service = new CartService();
  });

  it('adds a new item and updates totals', () => {
    service.addToCart(new CartItem(makeProduct(1, 10)));
    expect(service.cartItems.length).toBe(1);
    expect(service.totalQuantity.value).toBe(1);
    expect(service.totalPrice.value).toBe(10);
  });

  it('increments quantity when the same product is added again', () => {
    service.addToCart(new CartItem(makeProduct(1, 10)));
    service.addToCart(new CartItem(makeProduct(1, 10)));
    expect(service.cartItems.length).toBe(1);
    expect(service.cartItems[0].quantity).toBe(2);
    expect(service.totalQuantity.value).toBe(2);
    expect(service.totalPrice.value).toBe(20);
  });

  it('decrements quantity and removes the item at zero', () => {
    const item = new CartItem(makeProduct(1, 10));
    service.addToCart(item);
    service.decrementQuantity(item);
    expect(service.cartItems.length).toBe(0);
    expect(service.totalQuantity.value).toBe(0);
    expect(service.totalPrice.value).toBe(0);
  });

  it('computes totals across multiple items', () => {
    service.addToCart(new CartItem(makeProduct(1, 10)));
    service.addToCart(new CartItem(makeProduct(2, 5)));
    service.addToCart(new CartItem(makeProduct(2, 5)));
    expect(service.totalQuantity.value).toBe(3);
    expect(service.totalPrice.value).toBe(20);
  });

  it('clears the cart', () => {
    service.addToCart(new CartItem(makeProduct(1, 10)));
    service.clear();
    expect(service.cartItems.length).toBe(0);
    expect(service.totalQuantity.value).toBe(0);
    expect(service.totalPrice.value).toBe(0);
  });

  it('persists to sessionStorage and rehydrates a new instance', () => {
    service.addToCart(new CartItem(makeProduct(1, 10)));
    service.addToCart(new CartItem(makeProduct(2, 5)));

    const rehydrated = new CartService();
    expect(rehydrated.cartItems.length).toBe(2);
    expect(rehydrated.totalQuantity.value).toBe(2);
    expect(rehydrated.totalPrice.value).toBe(15);
  });
});
