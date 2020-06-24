package parser;
import java.io.*;
import lexer.*;
import symbols.*;
import inter.*;

public class Parser {

	private final Lexer lex;
	private Token look;
	Env top = null;
	int used = 0;

	public Parser(final Lexer l) throws IOException {
		lex = l;
		move();
	}

	void move() throws IOException {
		look = lex.scan();
	}

	void error(final String s) {
		throw new Error("near line " + lex.line + ": " + s);
	}

	void match(final int t) throws IOException {
		if (look.tag == t)
			move();
		else
			error("syntax error");
	}

	public void program() throws IOException {
		final Stmt s = block();
		final int begin = s.newlabel();
		final int after = s.newlabel();
		s.emitlabel(begin);
		s.gen(begin, after);
		s.emitlabel(after);
	}

	Stmt block() throws IOException {
		match('{');
		final Env savedEnv = top;
		top = new Env(top);
		decls();
		final Stmt s = stmts();
		match('}');
		top = savedEnv;
		return s;
	}

	void decls() throws IOException {
		while (look.tag == Tag.BASIC) {
			final Type p = type();
			final Token tok = look;
			match(Tag.ID);
			match(';');
			final Id id = new Id((Word) tok, p, used);
			top.put(tok, id);
			used = used + p.width;
		}
	}

	Type type() throws IOException {
		final Type p = (Type) look;
		match(Tag.BASIC);
		if (look.tag != '[') {
			return p;
		} else {
			return dims(p);
		}
	}

	Type dims(Type p) throws IOException {
		match('[');
		final Token tok = look;
		match(Tag.NUM);
		match(']');
		if (look.tag == '[') {
			p = dims(p);
		}
		return new Array(((Num) tok).value, p);
	}

	Stmt stmts() throws IOException {
		if (look.tag == '}')
			return Stmt.Null;
		else
			return new Seq(stmt(), stmts());
	}

	Stmt stmt() throws IOException {
		Expr x;
		final Stmt s;
		Stmt s1, s2, s3;
		Stmt savedStmt;
		switch (look.tag) {
		case ';':
			move();
			return Stmt.Null;
		case Tag.IF:
			match(Tag.IF);
			match('(');
			x = bool();
			match(')');
			s1 = stmt();
			if (look.tag != Tag.ELSE)
				return new If(x, s1);
			match(Tag.ELSE);
			s2 = stmt();
			return new Else(x, s1, s2);
		case Tag.WHILE:
			final While whilenode = new While();
			savedStmt = Stmt.enclosing;
			Stmt.enclosing = whilenode;
			match(Tag.WHILE);
			match('(');
			x = bool();
			match(')');
			s1 = stmt();
			whilenode.init(x, s1);
			Stmt.enclosing = savedStmt;
			return whilenode;
		case Tag.FOR:
			final For fornode = new For();
			savedStmt = Stmt.enclosing;
			Stmt.enclosing = fornode;
			match(Tag.FOR);
			match('(');
			s1 = stmt();
			x = bool();
			match(';');
			s2 = stmt();
			match(')');
			s3 = stmt();
			fornode.init(s1, x, s2, s3);
			Stmt.enclosing = savedStmt;
			return fornode;
		case Tag.DO:
			final Do donode = new Do();
			savedStmt = Stmt.enclosing;
			Stmt.enclosing = donode;
			match(Tag.DO);
			s1 = stmt();
			match(Tag.WHILE);
			match('(');
			x = bool();
			match(')');
			match(';');
			donode.init(s1, x);
			Stmt.enclosing = savedStmt;
			return donode;
		case Tag.BREAK:
			match(Tag.BREAK);
			match(';');
			return new Break();
		case '{':
			return block();
		default:
			return assign();
		}
	}

	Stmt assign() throws IOException {
		Stmt stmt;
		final Token t = look;
		match(Tag.ID);
		final Id id = top.get(t);
		if (id == null)
			error(t.toString() + " undeclared");
		if (look.tag == '=') {
			move();
			stmt = new Set(id, bool());
		} else if (look.tag == Tag.INC) {
			final Token tok = look;
			move();
			stmt = new Increment(id, tok);
		} else if (look.tag == Tag.DEC) {
			final Token tok = look;
			move();
			stmt = new Increment(id, tok);
		} else {
			final Access x = offset(id);
			match('=');
			stmt = new SetElem(x, bool());
		}

		match(';');
		return stmt;
	}

	Expr bool() throws IOException {
		Expr x = join();
		while (look.tag == Tag.OR) {
			final Token tok = look;
			move();
			x = new Or(tok, x, join());
		}

		return x;
	}

	Expr join() throws IOException {
		Expr x = equality();
		while (look.tag == Tag.AND) {
			final Token tok = look;
			move();
			x = new And(tok, x, equality());
		}
		return x;
	}

	Expr equality() throws IOException {
		Expr x = rel();
		while (look.tag == Tag.EQ || look.tag == Tag.NE) {
			final Token tok = look;
			move();
			x = new Rel(tok, x, rel());
		}
		return x;
	}

	Expr rel() throws IOException {
		final Expr x = expr();
		switch (look.tag) {
		case '<':
		case Tag.LE:
		case Tag.GE:
		case '>':
			final Token tok = look;
			move();
			return new Rel(tok, x, expr());
		default:
			return x;
		}
	}

	Expr expr() throws IOException {
		Expr x = term();
		while (look.tag == '+' || look.tag == '-') {
			final Token tok = look;
			move();
			x = new Arith(tok, x, term());
		}

		return x;
	}

	Expr term() throws IOException {
		Expr x = unary(); // tern()
		while (look.tag == '*' || look.tag == '/') {
			final Token tok = look;
			move();
			x = new Arith(tok, x, unary());
		}
		return x;
	}

	/*
	 * Expr tern() throws IOException { Expr x = unary(); while (look.tag == '(' ) {
	 * Token tok = look; move(); x= new Ternary(bool(), , unary()); } return x; }
	 */

	Expr unary() throws IOException {
		if (look.tag == '-') {
			move();
			return new Unary(Word.minus, unary());
		} else if (look.tag == '!') {
			final Token tok = look;
			move();
			return new Not(tok, unary());
		} else {
			return factor();
		}
	}

	Expr factor() throws IOException {
		Expr x = null;
		switch (look.tag) {
		case '(':
			move();
			x = bool();
			match(')');
			return x;
		case Tag.NUM:
			x = new Constant(look, Type.Int);
			move();
			return x;
		case Tag.REAL:
			x = new Constant(look, Type.Float);
			move();
			return x;
		case Tag.TRUE:
			x = Constant.True;
			move();
			return x;
		case Tag.FALSE:
			x = Constant.False;
			move();
			return x;
		default:
			error("syntax error");
			return x;
		case Tag.ID:
			final String s = look.toString();
			final Id id = top.get(look);
			if (id == null)
				error(look.toString() + " undeclared");
			move();
			if (look.tag != '[')
				return id;
			else
				return offset(id);
		}
	}

	Access offset(final Id a) throws IOException {
		Expr i; Expr w; Expr t1, t2; Expr loc;
		Type type = a.type;
		match('['); i = bool(); match(']');
		type = ((Array)type).of;
		w = new Constant(type.width);
		t1 = new Arith(new Token('*'), i, w);
		loc = t1;
		while( look.tag == '[') {
			match('['); i = bool(); match(']');
			type = ((Array)type).of;
			w = new Constant(type.width);
			t1 = new Arith(new Token('*'), i, w);
			t2 = new Arith(new Token('+'), loc, t1);
			loc = t2;
		}
		
		return new Access(a, loc, type);
	}
}