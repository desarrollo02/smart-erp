package py.com.logixone.plugins.sales.application;

import java.util.UUID;
import py.com.logixone.plugins.sales.api.*;
import py.com.logixone.plugins.sales.application.command.SalesCommands;
import py.com.logixone.plugins.sales.domain.*;

public interface SalesUseCases {
    SalesOperationResult<SalesTerm> createTerm(SalesOperationContext context, SalesCommands.CreateTerm command);
    SalesOperationResult<SalesTerm> reviseTerm(SalesOperationContext context, SalesCommands.ReviseTerm command);
    SalesOperationResult<SalesTerm> deactivateTerm(SalesOperationContext context, SalesCommands.DeactivateTerm command);
    SalesOperationResult<SalesQuote> createQuote(SalesOperationContext context, SalesCommands.CreateQuote command);
    SalesOperationResult<SalesQuote> issueQuote(SalesOperationContext context, SalesCommands.QuoteTransition command);
    SalesOperationResult<SalesQuote> rejectQuote(SalesOperationContext context, SalesCommands.QuoteTransition command);
    SalesOperationResult<SalesQuote> expireQuote(SalesOperationContext context, SalesCommands.QuoteTransition command);
    SalesOperationResult<SalesQuote> cancelQuote(SalesOperationContext context, SalesCommands.QuoteTransition command);
    SalesOperationResult<SalesOrder> acceptQuote(SalesOperationContext context, SalesCommands.AcceptQuote command);
    SalesOperationResult<SalesOrder> createOrder(SalesOperationContext context, SalesCommands.CreateOrder command);
    SalesOperationResult<SalesOrder> confirmOrder(SalesOperationContext context, SalesCommands.ConfirmOrder command);
    SalesOperationResult<SalesOrder> cancelOrder(SalesOperationContext context, SalesCommands.CancelOrder command);
    SalesOperationResult<SalesOrder> closeOrder(SalesOperationContext context, SalesCommands.CloseOrder command);
    SalesOperationResult<SalesQuoteReference> quote(SalesOperationContext context, SalesQuoteId id);
    SalesOperationResult<SalesOrderReference> order(SalesOperationContext context, SalesOrderId id);
}
