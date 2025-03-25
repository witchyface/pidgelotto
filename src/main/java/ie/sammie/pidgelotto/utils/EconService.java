package ie.sammie.pidgelotto.utils;

import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.accounts.Account;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.math.BigDecimal;
import java.util.UUID;

public class EconService {
    private static final EconomyService service = EconomyService.instance();

    public static Account getAccount(UUID uuid) {
        return service.account(uuid).join();
    }

    public static boolean hasBalance(UUID uuid, double amount) {
        Account account = getAccount(uuid);
        return account.balance().compareTo(BigDecimal.valueOf(amount)) >= 0;
    }

    public static boolean withdraw(UUID uuid, double amount) {
        Account account = getAccount(uuid);
        if (hasBalance(uuid, amount)) {
            account.withdraw(BigDecimal.valueOf(amount));
            return true;
        }
        return false;
    }

    public static String formatCurrency(double amount) {
        Component component = EconomyService.instance()
                .currencies()
                .primary()
                .format(BigDecimal.valueOf(amount));
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
