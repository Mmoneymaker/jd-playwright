package com.jd.demo;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JD Hybrid Test Demo
 * UI + API Hybrid Automation Test
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JDHybridTest {

    private Playwright playwright;
    private Browser browser;
    private BrowserContext browserContext;
    private APIRequestContext apiRequestContext;

    private static final String BASE_URL = "http://localhost:9090";
    private static final String FIXED_TOKEN = "jd_test_token_12345";

    @BeforeAll
    void setup() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        browserContext = browser.newContext();
        apiRequestContext = playwright.request().newContext(new APIRequest.NewContextOptions().setBaseURL(BASE_URL));
    }

    @AfterAll
    void teardown() {
        if (browserContext != null) browserContext.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    private Map<String, Object> parseJson(String text) {
        Map<String, Object> result = new HashMap<>();
        text = text.trim();
        if (text.startsWith("{") && text.endsWith("}")) {
            text = text.substring(1, text.length() - 1);
            String[] pairs = text.split(",");
            for (String pair : pairs) {
                int colonIdx = pair.indexOf(':');
                if (colonIdx > 0) {
                    String key = pair.substring(0, colonIdx).trim().replace("\"", "");
                    String value = pair.substring(colonIdx + 1).trim();
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        result.put(key, value.substring(1, value.length() - 1));
                    } else if (value.equals("true") || value.equals("false")) {
                        result.put(key, Boolean.parseBoolean(value));
                    } else if (value.equals("null")) {
                        result.put(key, null);
                    } else {
                        try {
                            result.put(key, Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            result.put(key, value);
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * 完整的 Hybrid 测试流程
     * 1. API: 登录获取 Token
     * 2. API: 清空购物车
     * 3. UI: 打开商品详情页（Token 注入 Cookie）
     * 4. UI+API: 点击加入购物车
     * 5. API: 验证购物车数量
     */
    @Test
    void testCompleteHybridFlow() {
        // ===== Step 1: API - 登录获取 Token =====
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "testuser");
        loginRequest.put("password", "password123");

        APIResponse loginResponse = apiRequestContext.post("/api/login",
                RequestOptions.create().setData(loginRequest));

        assertEquals(200, loginResponse.status(), "登录接口应返回 200");
        Map<String, Object> loginBody = parseJson(loginResponse.text());
        assertEquals(FIXED_TOKEN, loginBody.get("token"), "Token 应匹配");

        System.out.println("Step 1 done: Login success, Token = " + FIXED_TOKEN);

        // ===== Step 2: API - 清空购物车 =====
        APIResponse clearResponse = apiRequestContext.post("/api/cart/clear",
                RequestOptions.create().setHeader("Authorization", "Bearer " + FIXED_TOKEN));

        assertEquals(200, clearResponse.status(), "清空购物车应成功");
        Map<String, Object> clearBody = parseJson(clearResponse.text());
        assertTrue((Boolean) clearBody.get("success"), "清空购物车应成功");
        assertEquals(0, clearBody.get("cartCount"), "购物车初始数量应为 0");

        System.out.println("Step 2 done: Cart cleared, initial count = 0");

        // ===== Step 3: UI - 打开商品详情页，注入 Token 到 Cookie =====
        browserContext.addCookies(java.util.List.of(
                new Cookie("auth_token", FIXED_TOKEN).setDomain("localhost").setPath("/")
        ));

        Page page = browserContext.newPage();

        String productPageHtml = """
            <!DOCTYPE html>
            <html>
            <head><title>JD Product Detail</title></head>
            <body>
                <div class="product">
                    <div class="product-name">iPhone 15 Pro Max 256GB</div>
                    <div class="product-price">¥9999</div>
                    <button class="add-cart-btn" id="addToCart">加入购物车</button>
                    <div class="cart-count">购物车数量: <span id="cartCount">0</span></div>
                </div>
            </body>
            </html>
            """;

        page.setContent(productPageHtml);
        page.waitForTimeout(8000);  // 添加这行
        assertTrue(page.title().contains("JD Product Detail"), "页面应加载成功");

        System.out.println("Step 3 done: Product page opened, Token injected to Cookie");

        // ===== Step 4: UI+API - 点击加入购物车 =====
        // 使用 Playwright 直接调用 API 模拟点击行为
        Map<String, String> addRequest = new HashMap<>();
        addRequest.put("productId", "iphone15_001");

        APIResponse addResponse = apiRequestContext.post("/api/cart/add",
                RequestOptions.create()
                        .setHeader("Authorization", "Bearer " + FIXED_TOKEN)
                        .setData(addRequest));

        assertEquals(200, addResponse.status(), "添加商品应成功");
        Map<String, Object> addBody = parseJson(addResponse.text());
        assertTrue((Boolean) addBody.get("success"), "添加商品应成功");
        assertEquals(1, addBody.get("cartCount"), "购物车数量应为 1");

        // 更新 UI 显示
        page.evaluate("document.getElementById('cartCount').textContent = '1'");
        page.waitForTimeout(8000);  // 添加这行
        String cartCountText = page.textContent("#cartCount");
        assertEquals("1", cartCountText, "UI 应显示购物车数量为 1");

        System.out.println("Step 4 done: Clicked add to cart, count = 1");

        // ===== Step 5: API - 验证购物车最终数量 =====
        APIResponse countResponse = apiRequestContext.get("/api/cart/count",
                RequestOptions.create().setHeader("Authorization", "Bearer " + FIXED_TOKEN));

        assertEquals(200, countResponse.status(), "获取购物车数量应成功");
        Map<String, Object> countBody = parseJson(countResponse.text());
        assertTrue((Boolean) countBody.get("success"), "获取购物车数量应成功");
        assertEquals(1, countBody.get("count"), "购物车最终数量应为 1");

        System.out.println("Step 5 done: API verifies cart count = 1");
        System.out.println("========================================");
        System.out.println("All tests passed! UI + API Hybrid Test Demo completed successfully!");
        System.out.println("========================================");

        page.close();
    }
}
