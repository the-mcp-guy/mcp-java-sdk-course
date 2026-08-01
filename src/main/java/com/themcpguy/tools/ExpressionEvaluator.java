package com.themcpguy.tools;

import java.util.Map;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

/**
 * Tiny arithmetic expression evaluator used by the 'calculate' tool. Supports the
 * subset the tool's description advertises: +, -, *, /, ^, parentheses, and the
 * functions sqrt, abs, round, floor, ceil.
 *
 * Kept small on purpose. The string it evaluates arrives from a model, and may
 * originally have come from a user, so the less this understands the less it can
 * be talked into doing.
 *
 * Strict by design: anything it can't parse raises {@link IllegalArgumentException}
 * with a message suitable for surfacing back to the model in a tool-error result.
 */
final class ExpressionEvaluator {

    private static final Map<String, DoubleUnaryOperator> UNARY = Map.of(
        "sqrt", Math::sqrt,
        "abs",  Math::abs,
        "floor", Math::floor,
        "ceil", Math::ceil
    );

    private static final Map<String, DoubleBinaryOperator> BINARY = Map.of(
        "round", (v, places) -> {
            double scale = Math.pow(10, places);
            return Math.round(v * scale) / scale;
        }
    );

    private ExpressionEvaluator() {}

    static double evaluate(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("expression is empty");
        }
        return new Parser(expression).parse();
    }

    /** Recursive-descent parser. */
    private static final class Parser {
        private final String src;
        private int pos;

        Parser(String src) {
            this.src = src;
            this.pos = 0;
        }

        double parse() {
            double v = parseExpr();
            skipWhitespace();
            if (pos < src.length()) {
                throw new IllegalArgumentException(
                    "unexpected character '" + src.charAt(pos) + "' at position " + pos);
            }
            return v;
        }

        // expr = term (('+' | '-') term)*
        private double parseExpr() {
            double v = parseTerm();
            while (true) {
                skipWhitespace();
                if (consume('+')) v += parseTerm();
                else if (consume('-')) v -= parseTerm();
                else return v;
            }
        }

        // term = power (('*' | '/') power)*
        private double parseTerm() {
            double v = parsePower();
            while (true) {
                skipWhitespace();
                if (consume('*')) v *= parsePower();
                else if (consume('/')) {
                    double divisor = parsePower();
                    if (divisor == 0) throw new IllegalArgumentException("division by zero");
                    v /= divisor;
                } else return v;
            }
        }

        // power = unary ('^' power)?    (right-associative)
        private double parsePower() {
            double v = parseUnary();
            skipWhitespace();
            if (consume('^')) v = Math.pow(v, parsePower());
            return v;
        }

        // unary = ('-' | '+')? primary
        private double parseUnary() {
            skipWhitespace();
            if (consume('-')) return -parseUnary();
            if (consume('+')) return parseUnary();
            return parsePrimary();
        }

        // primary = number | '(' expr ')' | funcCall
        private double parsePrimary() {
            skipWhitespace();
            if (pos >= src.length()) throw new IllegalArgumentException("unexpected end of expression");
            char c = src.charAt(pos);

            if (consume('(')) {
                double v = parseExpr();
                skipWhitespace();
                if (!consume(')')) throw new IllegalArgumentException("expected ')' at position " + pos);
                return v;
            }

            if (Character.isLetter(c)) {
                return parseFunctionCall();
            }

            return parseNumber();
        }

        private double parseFunctionCall() {
            int start = pos;
            while (pos < src.length() && Character.isLetter(src.charAt(pos))) pos++;
            String name = src.substring(start, pos).toLowerCase();
            skipWhitespace();
            if (!consume('(')) throw new IllegalArgumentException("expected '(' after '" + name + "'");
            double arg1 = parseExpr();
            skipWhitespace();
            if (consume(',')) {
                double arg2 = parseExpr();
                skipWhitespace();
                if (!consume(')')) throw new IllegalArgumentException("expected ')' after second arg to '" + name + "'");
                DoubleBinaryOperator op = BINARY.get(name);
                if (op == null) throw new IllegalArgumentException("unknown 2-arg function '" + name + "'");
                return op.applyAsDouble(arg1, arg2);
            }
            if (!consume(')')) throw new IllegalArgumentException("expected ')' after arg to '" + name + "'");
            DoubleUnaryOperator op = UNARY.get(name);
            if (op == null) throw new IllegalArgumentException("unknown function '" + name + "'");
            return op.applyAsDouble(arg1);
        }

        private double parseNumber() {
            skipWhitespace();
            int start = pos;
            boolean sawDigit = false;
            boolean sawDot = false;
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (Character.isDigit(c)) { sawDigit = true; pos++; }
                else if (c == '.' && !sawDot) { sawDot = true; pos++; }
                else break;
            }
            if (!sawDigit) throw new IllegalArgumentException("expected number at position " + start);
            return Double.parseDouble(src.substring(start, pos));
        }

        private boolean consume(char expected) {
            skipWhitespace();
            if (pos < src.length() && src.charAt(pos) == expected) {
                pos++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
        }
    }

}
