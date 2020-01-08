package inter;

public class Stmt extends Node {
	public static Stmt Null = new Stmt();
	
	public void gen(int b, int a) {}			 //chamado com rótulos begin e agter
	int after = 0; 								//guarda rótulo after
	public static Stmt enclosing = Stmt.Null;	//usado para comando break
}
