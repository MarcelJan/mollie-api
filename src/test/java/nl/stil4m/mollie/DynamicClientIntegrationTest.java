package nl.stil4m.mollie;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.stil4m.mollie.domain.CreatePayment;
import nl.stil4m.mollie.domain.CreatedPayment;
import nl.stil4m.mollie.domain.Page;
import nl.stil4m.mollie.domain.Payment;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class DynamicClientIntegrationTest {

    private DynamicClient client;

    private String VALID_API_KEY = "test_nVK7W2WFmZXUNWcntBtCwvgCAgZ3c5";

    @Before
    public void before() {
        ObjectMapper mapper = new ObjectMapper();
        client = new DynamicClientBuilder()
                .withMapper(mapper).build();
    }

    @Test
    public void validateInvalidApiKey() throws IOException {
        assertThat(client.status("invalid").checkApiKey().getValid(), is(false));
    }

    @Test
    public void validateValidApiKey() throws IOException {
        assertThat(client.status(VALID_API_KEY).checkApiKey().getValid(), is(true));
    }

    @Test
    public void testCreatePayment() throws IOException {
        Date beforeTest = new Date();
        ResponseOrError<CreatedPayment> payment = client.payments(VALID_API_KEY).create(new CreatePayment(null, 1.00, "Some description", "http://example.com", null));

        assertWithin(beforeTest, payment.getData().getCreatedDatetime(), new Date());
    }

    public static void assertWithin(Date before, Date target, Date after) {
        long beforeTime = before.getTime() - (before.getTime() % 1000) - 1000; //Subtract another 1000 just to be safe
        long afterTime = after.getTime() - (after.getTime() % 1000) + 1000;
        assertThat(beforeTime <= target.getTime(), is(true));
        assertThat(target.getTime() <= afterTime, is(true));
    }

    @Test
    public void testGetPayment() throws IOException {
        ResponseOrError<CreatedPayment> payment = client.payments(VALID_API_KEY).create(new CreatePayment(null, 1.00, "Some description", "http://example.com", null));
        String id = payment.getData().getId();

        ResponseOrError<Payment> paymentStatus = client.payments(VALID_API_KEY).get(id);
        assertThat(paymentStatus.getData().getStatus(), is("open"));
    }

    @Test
    public void testGetPayments() throws IOException, URISyntaxException {
        ResponseOrError<Page<Payment>> result = client.payments(VALID_API_KEY).list(Optional.empty(), Optional.empty());
        assertThat(result.getSuccess(), is(true));
        Page<Payment> page = result.getData();

        assertThat(page.getTotalCount(), is(notNullValue()));
        assertThat(page.getData().size(), is(Math.min(10, page.getTotalCount())));
        assertThat(page.getOffset(), is(0));
        assertThat(page.getCount(), is(10));

        assertThat(page.getLinks().getPrevious().isPresent(), is(false));
        assertThat(page.getLinks().getNext().isPresent(), is(true));
        assertThat(page.getLinks().getLast().isPresent(), is(true));
        assertThat(page.getLinks().getFirst().isPresent(), is(true));
    }

    @Test
    public void testGetPaymentsWithOffsetAndCount() throws IOException, URISyntaxException {
        ResponseOrError<Page<Payment>> result = client.payments(VALID_API_KEY).list(Optional.of(20), Optional.of(40));
        assertThat(result.getSuccess(), is(true));
        Page<Payment> page = result.getData();

        assertThat(page.getTotalCount(), is(notNullValue()));
        assertThat(page.getData().size(), is(Math.min(20, page.getTotalCount())));
        assertThat(page.getOffset(), is(40));
        assertThat(page.getCount(), is(20));

        assertThat(page.getLinks().getPrevious().isPresent(), is(true));
        assertThat(page.getLinks().getNext().isPresent(), is(true));
        assertThat(page.getLinks().getLast().isPresent(), is(true));
        assertThat(page.getLinks().getFirst().isPresent(), is(true));
    }

    @Test
    public void testNextPayments() throws IOException, URISyntaxException {
        ResponseOrError<Page<Payment>> result = client.payments(VALID_API_KEY).list(Optional.empty(), Optional.empty());
        assertThat(result.getSuccess(), is(true));
        Page<Payment> page = result.getData();

        Page<Payment> nextPage = client.payments(VALID_API_KEY).next(page).getData();

        assertThat(nextPage.getOffset(), is(10));
        assertThat(nextPage.getCount(), is(10));
    }

    @Test
    public void testPreviousPayments() throws IOException, URISyntaxException {
        ResponseOrError<Page<Payment>> result = client.payments(VALID_API_KEY).list(Optional.of(20), Optional.of(40));
        assertThat(result.getSuccess(), is(true));
        Page<Payment> page = result.getData();

        Page<Payment> previousPage = client.payments(VALID_API_KEY).previous(page).getData();

        assertThat(previousPage.getOffset(), is(20));
        assertThat(previousPage.getCount(), is(20));
    }
}