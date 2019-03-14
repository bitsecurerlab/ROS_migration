package cost_matrix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Test {
	
	private static HashMap<Integer, String> files = new HashMap<>();
	private static double[][] Smatrix;
	
	/**
	 * �Ƚϰ���������ƶ�
	 */	
	
	public static void main(String[] args) throws Exception {
		Test test = new Test();
		String path1 = "file.txt";
		String path2 = "test.txt";
		String path3 = "target.txt";
		ArrayList<String> targetfile = new ArrayList<String>();
		test.ReadFile(path1, path2);
		File file = new File(path3);
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = "";
		while((line=br.readLine())!=null){
			String tmp = line;
			targetfile.add(tmp);
		}
		br.close();
//		for(int i=0;i<targetfile.size();i++){
//			String tmp = targetfile.get(i);
//			double avg = test.avgS(tmp);//������ͬ�ļ������ƶ�
//			System.out.println("avg:"+avg);
//			System.out.println("--------------");
//		}
		double[][] matrix = new double[targetfile.size()][targetfile.size()];
		for(int i=0;i<targetfile.size();i++){
			String name1 = targetfile.get(i);
			for(int j=0;j<targetfile.size();j++){
				String name2 = targetfile.get(j);
				if(name1.equals(name2))
					continue;
				double avg = test.avgS(name1, name2);//���㲻ͬ�ļ��м����ƶ�
				System.out.println("name1:"+name1+" name2:"+name2);
				System.out.println("avg:"+avg);
				System.out.println("--------------");
				matrix[i][j] = avg;
			}
		}
		
		String path4 = "matrix.csv";
		File file2 = new File(path4);
		BufferedWriter wr = new BufferedWriter(new FileWriter(file2));
		for(int i=0;i<targetfile.size();i++){
			wr.append(targetfile.get(i)+",");
		}
		wr.newLine();
		wr.flush();
		for(int i=0;i<targetfile.size();i++){
			for(int j=0;j<targetfile.size();j++){
				if(i==j){
					wr.append("1,");
					continue;
				}
				wr.append(String.valueOf(matrix[i][j])+",");
			}
			wr.newLine();
			wr.flush();
		}//����������ƶȾ���
		wr.close();
		
		String path5 = "matrix1.csv";
		File file3 = new File(path5);
		BufferedWriter wr1 = new BufferedWriter(new FileWriter(file3));
		for(int i=0;i<targetfile.size();i++){
			double avg = 0.0;
			double max = 0.0;
			double min = 1.0;
			String maxname = "";
			for(int j=0;j<targetfile.size();j++){
				double tmp = matrix[i][j];
				if(i==j)
					continue;
				avg = avg+tmp;
				if(tmp>max){
					max = tmp;
					maxname = targetfile.get(j);
				}
				if(tmp<min)
					min = tmp;
			}
			avg = avg/(targetfile.size()-1);//ƽ���������ƶ�
			wr1.append(targetfile.get(i)+",");
			wr1.append(String.valueOf(avg)+",");
			wr1.append(String.valueOf(max)+",");
			wr1.append(maxname+",");
			wr1.append(String.valueOf(min)+",");
			wr1.newLine();
			wr1.flush();
		}
		wr1.close();
	}
	
	public void ReadFile(String path1, String path2) throws IOException{
		File file = new File(path1);
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = "";
		while((line=br.readLine())!=null){
			String[] lines = line.split(",");
			files.put(Integer.valueOf(lines[0]), lines[1]);			
		}
		br.close();
		
		Smatrix = new double[files.size()][files.size()];
		File file1 = new File(path2);
		BufferedReader br1 = new BufferedReader(new FileReader(file1));
		String line1 = "";
		int num = 0;
		while((line1=br1.readLine())!=null){
			String[] lines = line1.split(",");
			for(int i=0;i<lines.length-1;i++){
				String tmp = lines[i];
				if(tmp.substring(0, 1).equals(" ")){
					double sm = Double.valueOf(tmp.substring(1, tmp.length()));
					Smatrix[num][i] = sm;
				}else {
					double sm = Double.valueOf(tmp);
					Smatrix[num][i] = sm;
				}
			}
			num++;
		}
		br1.close();
	}//��ȡ���ƶȾ�����ļ���
	
	public double avgS(String name1) throws Exception{
		ArrayList<Double> avgs = new ArrayList<>();
		ArrayList<Integer> files1 = new ArrayList<>();
		for(Entry<Integer, String> entry : files.entrySet()){
			int key = entry.getKey();
			String value = entry.getValue();
			if(value.contains(name1)){
				files1.add(key);
			}
		}//�����ж��������ļ�����Ӧ��Ϣ
		System.out.println("size:"+files1.size());	
		if(files1.isEmpty())
			throw new Exception("����Ϊ�գ�");
		
		double avg = 0.0;
		for(int i=0;i<files1.size();i++){
			int x = files1.get(i);
			for(int j=0;j<files1.size();j++){
				int y = files1.get(j);
				if(Smatrix[x][y]==1.0)
					continue;
				avg = avg+Smatrix[x][y];
			}			
		}
		avg = avg/(files1.size()*(files1.size()-1));	
		return avg;
	}//����ͬ�ļ���֮�����ƶ�
	
	public double avgS(String name1, String name2) throws Exception{
		ArrayList<Integer> files1 = new ArrayList<>();
		ArrayList<Integer> files2 = new ArrayList<>();
		ArrayList<Double> avgs = new ArrayList<>();	
		for(Entry<Integer, String> entry : files.entrySet()){
			int key = entry.getKey();
			String value = entry.getValue();
			if(value.contains(name1)){
				files1.add(key);
			}
			if(value.contains(name2)){
				files2.add(key);
			}
		}//�����ж��������ļ�����Ӧ��Ϣ
		if(files1.isEmpty()||files2.isEmpty())
			throw new Exception("����Ϊ�գ�");
		
		for(int i=0;i<files1.size();i++){
			double avg = 0.0;
			int x = files1.get(i);
			for(int j=0;j<files2.size();j++){
				int y = files2.get(j);
				avg= avg+Smatrix[x][y];
			}
			for(int k=0;k<files2.size();k++){
				int y = files2.get(k);
				avg= avg+Smatrix[y][x];
			}
			avg = avg/(2*files2.size());//�˴�����-1
			System.out.println(avg);
			avgs.add(avg);	
		}
		double avg = 0.0;
		for(int i=0;i<avgs.size();i++){
			double tmp = avgs.get(i);
			avg = avg+tmp;
		}
		avg = avg/files1.size();
		return avg;
	}//���㲻ͬ�ļ���֮�����ƶ�

}
